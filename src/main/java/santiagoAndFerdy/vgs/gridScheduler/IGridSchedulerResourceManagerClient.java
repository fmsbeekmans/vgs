package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.messages.MonitorRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerResourceManagerClient extends Remote {
    /**
     * UserRequest this grid scheduler to monitor the life-cycle for this job
     * @param monitorRequest the request to watch
     * @throws RemoteException
     */
    Promise<Void> monitorPrimary(MonitorRequest monitorRequest) throws RemoteException;

    /**
     * For a resource manager to userRequest a job to be scheduled elsewhere
     * @param userRequest to schedule somewhere else
     */
    Promise<Void> offload(UserRequest userRequest);

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    Promise<Void> releaseResources(int requestId);
}
