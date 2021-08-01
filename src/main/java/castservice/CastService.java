package castservice;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * CastService - implementation "X" of ICastService.
 * see@ castservice.ICastService
 */
class CastService implements ICastService, Runnable {
    final static String VERSION = "1.0.0"; // TODO: embed git commit id
    final int INPUT_QUEUE_SIZE_MAX = Integer.parseInt(System.getProperty("castservice.INPUT_QUEUE_MAX_SIZE",
            "1000"));
    final int TARGETS_NUM_HINT = Integer.parseInt(System.getProperty("castservice.TARGETS_NUM_HINT",
            "100"));
    final int CASTS_NUM_HINT = Integer.parseInt(System.getProperty("castservice.CASTS_NUM_HINT",
            "10000"));
    final int SERVICE_PORT = Integer.parseInt(System.getProperty("castservice.SERVICE_PORT",
        Integer.toString(ICastService.REGISTRY_PORT)));

    // "main" incoming Cast queue. all incoming Casts are going through this queue to make sendCast() fast.
    BlockingQueue<Cast> incomingQueue_ = new LinkedBlockingQueue<>(INPUT_QUEUE_SIZE_MAX);

    // "targets" map: targetId -> Target, all targets learned from Casts.
    Map<Long, Target> targets_ = new ConcurrentHashMap<>(TARGETS_NUM_HINT);

    // "Casts" map: Cast id -> Cast, all active casts (references) in service. used by cancelCast().
    Map<String, Cast> activeCasts_ = new ConcurrentHashMap<>(CASTS_NUM_HINT);

    volatile boolean isShutdown_ = false;
    Thread mainThread_;

    /////////////////////////////////////////////////////////////////////

    public boolean isShutdown() { return isShutdown_; }

    /**
     * returns Service interface version. Version should change:
     *  - Cast changed
     *  - ICastService changed
     * @return version string
     * @throws RemoteException
     */

    public String interfaceVersion() throws RemoteException
    {
        return ICastService.VERSION;
    }

    /**
     * Register service object in local RMI registry and start serving requests.
     * Registry port ICastService.REGISTRY_PORT is not sharable.
     * @throws RemoteException
     */
    public void register() throws RemoteException, ServiceErrorException
    {
        // start incoming queue loop first
        mainThread_ = new Thread(this);
        mainThread_.start();
        // register this instance in local RMI registry
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(SERVICE_PORT);
        } catch (Exception e) {
            throw new ServiceErrorException(e);
        }
        ICastService stub = (ICastService)UnicastRemoteObject.exportObject(this, 0);
        registry.rebind(RMI_NAME, stub);
        System.out.println("ready to accept requests");
    }

    void shutdown() throws InterruptedException
    {
        System.out.println("shutdown() called...");
        isShutdown_ = true;
        mainThread_.join();
    }

    /**
     * Incoming queue loop
     */
    public void run()
    {
        while(!isShutdown_)
        {
            try
            {
                Cast cast = incomingQueue_.poll(100, TimeUnit.MILLISECONDS);
                if(cast == null)
                    continue;
                assert(cast.isValid());
                if(cast.status_ == 0 /*Canceled*/)
                    // remove cast from "global" book.
                    // "target" books will remove it in getActiveCasts() eventually
                    activeCasts_.remove(cast.id());
                else if(cast.status_ == 1 /*Active*/)
                    // put cast into "global" book. this add/replace using Cast.id().
                    activeCasts_.put(cast.id(), cast);
                System.out.printf("activeCasts: added [%s]\n", cast);
                // loop over cast targets
                for(long targetUserId: cast.targetUserIds_)
                {
                    Target target = targets_.get(targetUserId);
                    if(target == null)
                        target = new Target(targetUserId);
                        targets_.put(targetUserId, target);
                    // put cast into "target" book and
                    // add cast to stream queues if any
                    // this shall be be fast - O(C)
                    target.sendCast(cast);
                }
            }
            catch(InterruptedException ignore) { }
            catch(ServiceErrorException e) {
                e.printStackTrace();
                isShutdown_ = true;
            }
        }
        System.out.println("Exiting...");
    }

    public String createTicket(long id)
    {
        long now = System.currentTimeMillis();
        return String.format("%d:%d", id, now);
    }

    /////////////////////////////////////////////////////////////////// ICastService

    /**
     * @see castservice.ICastService
     */
    public String sendCast(Cast cast) throws InvalidParamException, RemoteException, ServiceErrorException
    {
        // params validation
        if(!cast.isValid())
            throw new InvalidParamException(cast.toString());
        // check for queue overflow
        if(incomingQueue_.size() == INPUT_QUEUE_SIZE_MAX)
            throw new ServiceErrorException(String.format("Service input queue overrun: [%d]", INPUT_QUEUE_SIZE_MAX));
        // enqueue and return fast
        incomingQueue_.add(cast);
        String ticket = createTicket(cast.originatorUserId_);
        System.out.printf("sendCast: %s [%s]\n", cast, ticket);
        return ticket;
    }

    /**
     * @see castservice.ICastService
     */
    public String cancelCast(long originatorUserId, long bondId, int side)
            throws CastNotFoundException, InvalidParamException, RemoteException
    {
        // params validation
        if(!Cast.isValidOriginatorUserId(originatorUserId))
            throw new InvalidParamException(String.format("originatorUserId=%d", originatorUserId));
        if(!Cast.isValidBondId(bondId))
            throw new InvalidParamException(String.format("bondId=%d", bondId));
        if(!Cast.isValidSide(side))
            throw new InvalidParamException(String.format("side=%d", side));
        String id = Cast.createId(originatorUserId, bondId, side);
        // remove from "all" cast map
        Cast cast = activeCasts_.remove(id);
        if(cast == null)
            throw new CastNotFoundException();
        assert(cast.isValid());
        // mark cast as Canceled
        // target's cast maps will see it as canceled, and will remove it in getActiveCasts()
        cast.status_ = 0; // Canceled
        // TODO: add bg thread to remove canceled.
        //  Consider weak refs.
        String ticket = createTicket(originatorUserId);
        System.out.printf("cancelCast: %s [%s]\n", cast, ticket);
        return ticket;
    }

    /**
     * @see castservice.ICastService
     */
    public Cast[] getActiveCasts(long targetUserId) throws InvalidParamException, RemoteException
    {
        System.out.printf("getActiveCasts: targetUserId=%d\n", targetUserId);
        // params validation
        if(!Cast.isValidTargetUserId(targetUserId))
            throw new InvalidParamException(String.format("targetUserId=%d", targetUserId));
        // clone active Casts from target book
        // and filter final cloned list for canceled
        List<Cast> activeCasts = new LinkedList<>();
        Target target = targets_.get(targetUserId);
        if(target == null)
            return new Cast[] {};  // there was no any Casts registered for this target yet
        // loop over active Casts, which may be canceled
        for(Map.Entry<String, Cast> pair: target.activeCasts_.entrySet())
        {
            Cast cast = (Cast)pair.getValue().clone();
            assert(cast.isValid());
            if(cast.status_ == 1/*Active*/)
                activeCasts.add((Cast)cast.clone());
            else
                target.activeCasts_.remove(pair.getKey());  // remove non-active casts from target's book
        }
        System.out.printf("getActiveCasts: targetUserId=%d, casts=%d\n", targetUserId, activeCasts.size());
        return (Cast[])activeCasts.toArray(new Cast[activeCasts.size()]);
    }

    /**
     * @see castservice.ICastService
     */
    public int streamCasts(long targetUserId) throws InvalidParamException,
            ServiceErrorException, RemoteException
    {
        if(!Cast.isValidTargetUserId(targetUserId))
            throw new InvalidParamException(String.format("targetUserId=%d", targetUserId));
        Target target = targets_.get(targetUserId);
        if(target == null)
        {
            target = new Target(targetUserId);
            targets_.put(targetUserId, target);
        }
        int port = target.streamCasts(); // port for client to connect to
        return port;
    }
}
