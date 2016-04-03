package santiagoAndFerdy.vgs.gridScheduler;

import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.discovery.ICrashable;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IGridScheduler extends ICrashable {
    /**
     * Request this grid scheduler to monitor the life-cycle for this job
     * 
     * @param monitorRequest
     *            the request to watch
     * @throws RemoteException
     */
    void monitor(MonitoringRequest monitorRequest) throws RemoteException;

    /**
     * Request this grid scheduler to also watch a job in case the primary grid scheduler crashes
     * 
     * @param backUpRequest
     *            the userRequest to watch
     * @throws RemoteException
     */
    void backUp(BackUpRequest backUpRequest) throws RemoteException;

    /**
     * For a resource manager to request a job to be scheduled elsewhere
     * 
     * @param workRequest
     *            to schedule somewhere else
     */
    void offLoad(WorkRequest workRequest) throws RemoteException;

    /**
     * A job has finished. All the reserved resources can be released.
     * 
     * @param workRequest - the workRequest
     */
    void releaseMonitored(WorkRequest workRequest) throws RemoteException;

    /**
     * A job has finished. No need to watch as back-up anymore.
     * @param workRequest to release
     * @throws RemoteException
     */
    void releaseBackUp(WorkRequest workRequest) throws RemoteException;
}
