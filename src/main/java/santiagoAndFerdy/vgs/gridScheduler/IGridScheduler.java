
package santiagoAndFerdy.vgs.gridScheduler;

import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.discovery.ICrashable;
import santiagoAndFerdy.vgs.messages.*;

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
    void monitor(MonitorRequest monitorRequest) throws RemoteException;

    /**
     * Request this grid scheduler to also watch a job in case the primary grid scheduler crashes
     *
     * @param backUpRequest
     *            the userRequest to watch
     * @throws RemoteException
     */
    void backUp(BackUpRequest backUpRequest) throws RemoteException;

    void acceptBackUpAck(BackUpAck ack) throws RemoteException;
    /**
     * Become the monitor insteadd of back up for this workRequest
     * @param promotionRequest
     * @throws RemoteException
     */
    void promote(PromotionRequest promotionRequest) throws RemoteException;

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

    /**
     * a resource manager has woken up and announces it's presence
     * @param from
     * @throws RemoteException
     */
    void receiveResourceManagerWakeUpAnnouncement(int from) throws RemoteException;

    /**
     * a grid scheduler has woken up and announces it's presence
     * @param from
     * @throws RemoteException
     */
    void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException;
}