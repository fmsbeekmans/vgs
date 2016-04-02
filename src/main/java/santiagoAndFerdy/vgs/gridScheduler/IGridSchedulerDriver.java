package santiagoAndFerdy.vgs.gridScheduler;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.IAdressable;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IGridSchedulerDriver extends IAdressable {
    void ping(@NotNull Heartbeat h) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;

    /**
     * UserRequest this grid scheduler to monitor the life-cycle for this job
     * @param monitorRequest the userRequest to watch
     * @throws RemoteException
     */
    void monitorPrimary(MonitoringRequest monitorRequest) throws RemoteException, MalformedURLException, NotBoundException;

    /**
     * UserRequest this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param backUpRequest the userRequest to watch
     * @throws RemoteException
     */
    void monitorBackUp(BackUpRequest backUpRequest) throws RemoteException, MalformedURLException, NotBoundException;

    /**
     * For a resource manager to userRequest a job to be scheduled elsewhere
     * @param userRequest to schedule somewhere else
     */
    void offload(UserRequest userRequest) throws RemoteException;

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    void releaseResources(int requestId) throws RemoteException;
}
