package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.model.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerGridSchedulerClient extends Remote {
    /**
     * Request this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param request the request to watch
     * @throws RemoteException
     */
    Promise<Void> manitorBackUp(Request request) throws RemoteException;

    /**
     * Become the primary grid scheduler for this job
     * @param request the request to reschedule and rebackup
     */
    void promoteToPrimary(Request request);

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    Promise<Void> releaseResources(int requestId);
}
