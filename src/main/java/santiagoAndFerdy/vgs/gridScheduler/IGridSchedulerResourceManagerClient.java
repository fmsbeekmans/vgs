package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.discovery.IAdressable;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.model.Job;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerResourceManagerClient extends IAdressable {
    /**
     * UserRequest this grid scheduler to monitor the life-cycle for this job
     * @param job to oversee
     * @throws RemoteException
     */
    Promise<Void> monitorPrimary(Job job) throws RemoteException, MalformedURLException, NotBoundException;

    /**
     * For a Grid Scheduler Driver to communicate that a monitoring request has been accepted.
     * @param job that is being monitored
     */
    void monitoringRequestAccepted(Job job) throws RemoteException;

    /**
     * For a resource manager to userRequest a job to be scheduled elsewhere
     * @param userRequest to schedule somewhere else
     */
    Promise<Void> offload(UserRequest userRequest) throws RemoteException;

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    Promise<Void> releaseResources(int requestId) throws RemoteException;
}
