package castservice;

import java.rmi.RemoteException;

/**
 * Remote Interface
 */
public interface ICastService extends java.rmi.Remote {
    String RMI_NAME = "CastService";
    int REGISTRY_PORT = 11001;
    String VERSION = "1.0";

    /**
     * Returns interface version.
     * @return version string
     * @throws RemoteException
     */
    String interfaceVersion() throws RemoteException;

    /**
     * Send a Cast to target users
     * @param cast
     * @return  String, ticket id
     * @throws InvalidParamException, RemoteException
     */
    String sendCast(Cast cast) throws InvalidParamException, RemoteException, ServiceErrorException;

    /**
     * Cancel specific Cast - remove from the active Casts book
     * @param originatorUserId
     * @param bondId
     * @param side
     * @return ticket id  String, ticket id
     * @throws CastNotFoundException, InvalidParamException, RemoteException
     */
    String cancelCast(long originatorUserId, long bondId, int side) throws CastNotFoundException,
            InvalidParamException, RemoteException;

    /**
     * Get snapshot of the all active Casts for specific targetUserId
     * @param targetUserId
     * @return  Cast array, empty if nothing found
     * @throws InvalidParamException, RemoteException
     */
    Cast[] getActiveCasts(long targetUserId) throws InvalidParamException, RemoteException;

    /**
     * Create Cast Stream. Returns port number that client needs to connect to.
     * Semantic is Service implementation specific. CastService Streamer implementation uses OutputObjectStream over
     * TCP socket.
     * @see Client for reference implementation.
     * @param targetUserId
     * @return port number of socket server (Cast stream)
     * @throws InvalidParamException, RemoteException
     */
    int streamCasts(long targetUserId) throws InvalidParamException, RemoteException, ServiceErrorException;
}

