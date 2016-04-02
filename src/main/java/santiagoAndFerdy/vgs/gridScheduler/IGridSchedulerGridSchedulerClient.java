package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.discovery.IAddressable;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.model.Job;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IGridSchedulerGridSchedulerClient extends IAddressable {
    /**
     * Request this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * @param job to monitor
     * @throws RemoteException
     */
    Promise<Void> monitorBackUp(Job job) throws RemoteException, MalformedURLException, NotBoundException;

    void monitorBackUpAccepted(Job job) throws RemoteException;

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
