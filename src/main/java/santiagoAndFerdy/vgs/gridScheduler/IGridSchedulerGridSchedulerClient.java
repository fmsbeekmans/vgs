package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.discovery.Entity;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerGridSchedulerClient extends Entity {
    /**
     * UserRequest this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param backUpRequest the userRequest to watch
     * @throws RemoteException
     */
    Promise<Void> monitorBackUp(BackUpRequest backUpRequest) throws RemoteException;

    /**
     * Become the primary grid scheduler for this job
     * @param userRequest the userRequest to reschedule and rebackup
     */
    void promoteToPrimary(UserRequest userRequest) throws  RemoteException;

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    Promise<Void> releaseResources(int requestId) throws RemoteException;
}
