package santiagoAndFerdy.vgs.gridScheduler;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.model.Request;

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
     * Request this grid scheduler to monitor the life-cycle for this job
     * @param request the request to watch
     * @throws RemoteException
     */
    void monitorPrimary(Request request) throws RemoteException;

    /**
     * Request this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param request the request to watch
     * @throws RemoteException
     */
    void manitorBackUp(Request request) throws RemoteException;

    /**
     * Become the primary grid scheduler for this job in case tho previous primary grid scheduler or resource manager crash
     * @param request the request to reschedule and rebackup
     */
    void promoteToPrimary(Request request);

    /**
     * For a resource manager to request a job to be scheduled elsewhere
     * @param request to schedule somewhere else
     */
    void offload(Request request);

    /**
     * A job has finished. All the reserved resources can be released.
     * @param requestId
     */
    void releaseResources(int requestId);
}
