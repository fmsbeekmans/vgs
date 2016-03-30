package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.model.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerResourceManagerClient extends Remote {
    /**
     * Request this grid scheduler to monitor the life-cycle for this job
     * @param request the request to watch
     * @throws RemoteException
     */
    Promise<Void> monitorPrimary(Request request) throws RemoteException;

    /**
     * Become the primary grid scheduler for this job
     * @param request the request to reschedule and rebackup
     */
    Promise<Void> promoteToPrimary(Request request);

    /**
     * For a resource manager to request a job to be scheduled elsewhere
     * @param request to schedule somewhere else
     */
    Promise<Void> offload(Request request);

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    Promise<Void> releaseResources(int requestId);
}
