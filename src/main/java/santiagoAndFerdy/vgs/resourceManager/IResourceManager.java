package santiagoAndFerdy.vgs.resourceManager;

import santiagoAndFerdy.vgs.discovery.ICrashable;
import santiagoAndFerdy.vgs.messages.WorkOrder;
import santiagoAndFerdy.vgs.messages.WorkRequest;

import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IResourceManager extends ICrashable {
    /**
     * For users to offer jobs to the resource manager
     * @param req the workload offered to the resource manager
     */
    void offerWork(WorkRequest req) throws RemoteException;

    void orderWork(WorkOrder req) throws RemoteException;

    /**
     * For a node to signal that the processing of a job is finished
     * @param finished the work request that has finished
     * @throws RemoteException
     */
    void finish(Node node, WorkRequest finished) throws RemoteException;

    /**
     * a grid scheduler has woken up and announces it's presence
     * @param from
     * @throws RemoteException
     */
    void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException;
}
