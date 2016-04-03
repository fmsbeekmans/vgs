package santiagoAndFerdy.vgs.resourceManager;

import com.sun.istack.internal.NotNull;

import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.user.IUser;

/**
 * Created by Fydio on 3/19/16.
 */
public class ResourceManager extends UnicastRemoteObject implements IResourceManager {
    private RmiServer rmiServer;
    private int id;
    private IRepository<IUser> userRepository;
    private IRepository<IResourceManager> resourceManagerRepository;
    private IRepository<IGridScheduler> gridSchedulerRepository;

    private Queue<WorkRequest> jobQueue;
    private Queue<Node> idleNodes;
    private int nNodes;

    private ScheduledExecutorService timer;

    public ResourceManager(
            RmiServer rmiServer,
            int id,
            IRepository<IUser> userRepository, // TODO use appendable user repository.
            IRepository<IResourceManager> resourceManagerRepository,
            IRepository<IGridScheduler> gridSchedulerRepository,
            int nNodes) throws RemoteException {
        this.rmiServer = rmiServer;
        this.id = id;
        this.userRepository = userRepository;
        this.resourceManagerRepository = resourceManagerRepository;
        this.gridSchedulerRepository = gridSchedulerRepository;
        rmiServer.register(resourceManagerRepository.getUrl(id), this);

        this.nNodes = nNodes;
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new ArrayBlockingQueue<Node>(nNodes);

        this.timer = Executors.newSingleThreadScheduledExecutor();

        for(int i = 0; i < nNodes; i++) {
            Node node = new Node(i, this, timer);
            idleNodes.add(node);
        }
    }

    @Override
    public void offerWork(WorkRequest req) throws RemoteException {
        if (needToOffload()) {
            // TODO send to GS to offload
        } else {
            schedule(req);
            processQueue();
        }
    }

    protected boolean needToOffload() {
        return false;
    }

    @Override
    public void schedule(@NotNull WorkRequest toSchedule) throws RemoteException {
        System.out.println("Scheduling job " + toSchedule.getJob().getJobId());
        jobQueue.offer(toSchedule);
    }

    @Override
    public void finish(Node node, WorkRequest finished) throws RemoteException {
        Optional<IUser> maybeUser = userRepository.getEntity(finished.getUserId());

        if(maybeUser.isPresent()) {
            maybeUser.get().acceptResult(finished.getJob());
            release(finished);
            node.setIdle();
            idleNodes.add(node);
        } else {
            // TODO What to do when the user isn't there to accept the result?
        }

        processQueue();
    }

    public void release(WorkRequest req) throws RemoteException {

    }

    protected void processQueue() {
        while (true) {
            Node node = idleNodes.poll();
            if(node != null) {
                WorkRequest work = jobQueue.poll();
                if(work != null) {
                    node.handle(work);
                }
                else {
                    idleNodes.offer(node);
                    break;
                }
            } else {
                break;
            }

        }
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {

    }
}