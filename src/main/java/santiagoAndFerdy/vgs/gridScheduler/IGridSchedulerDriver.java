package santiagoAndFerdy.vgs.gridScheduler;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.model.BackUpRequest;
import santiagoAndFerdy.vgs.model.MonitorRequest;
import santiagoAndFerdy.vgs.model.UserRequest;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IGridSchedulerDriver extends Remote {
    void ping(@NotNull Heartbeat h) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;

    /**
     * UserRequest this grid scheduler to monitor the life-cycle for this job
     * @param userRequest the userRequest to watch
     * @throws RemoteException
     */
    void monitorPrimary(MonitorRequest userRequest) throws RemoteException;

    /**
     * UserRequest this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param userRequest the userRequest to watch
     * @throws RemoteException
     */
    void monitorBackUp(BackUpRequest userRequest) throws RemoteException;

    /**
     * For a resource manager to userRequest a job to be scheduled elsewhere
     * @param userRequest to schedule somewhere else
     */
    void offload(UserRequest userRequest);

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    void releaseResources(int requestId);
}
