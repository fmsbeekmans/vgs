package santiagoAndFerdy.vgs.resourceManager;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.ICrashable;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.WorkRequest;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
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

    /**
     * For a grid scheduler to schedule a job on the cluster managed by this resource maneger
     * @param toSchedule
     * @throws RemoteException
     * @throws MalformedURLException
     * @throws NotBoundException
     */
    void schedule(@NotNull WorkRequest toSchedule) throws RemoteException;

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
