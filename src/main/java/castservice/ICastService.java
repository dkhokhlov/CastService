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
     * @return
     * @throws InvalidParamException, RemoteException
     */
    String sendCast(Cast cast) throws InvalidParamException, RemoteException, ServiceErrorException;  // return ticket id

    /**
     * Cancel specific Cast - remove from the active Casts book
     * @param originatorUserId
     * @param bondId
     * @param side
     * @return ticket id
     * @throws CastNotFoundException, InvalidParamException, RemoteException
     */
    String cancelCast(long originatorUserId, long bondId, int side) throws CastNotFoundException,
            InvalidParamException, RemoteException;

    /**
     * Get snapshot of the all active Casts for specific targetUserId
     * @param targetUserId
     * @return
     * @throws InvalidParamException, RemoteException
     */
    Cast[] getActiveCasts(long targetUserId) throws InvalidParamException, RemoteException;

    /**
     *
     * @param originatorUserId
     * @return port number of socket server (Cast stream)
     * @throws InvalidParamException, RemoteException
     */
    int streamCasts(long originatorUserId) throws InvalidParamException, RemoteException, ServiceErrorException; // return stream port
}

