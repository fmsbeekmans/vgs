package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;

import santiagoAndFerdy.vgs.discovery.Pinger;
import santiagoAndFerdy.vgs.discovery.Status;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
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
    private Pinger<IGridScheduler> gridSchedulerPinger;

    private Queue<WorkRequest> jobQueue;
    private Queue<Node> idleNodes;
    private int nNodes;

    private Map<WorkRequest, Integer> monitoredBy;
    private Map<WorkRequest, Integer> backedUpAt;

    private ScheduledExecutorService timer;
    private Engine engine;

    private boolean running;

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
        gridSchedulerPinger = new Pinger(gridSchedulerRepository);

        this.nNodes = nNodes;
        running = false;

        start();
    }

    @Override
    public void offerWork(WorkRequest req) throws RemoteException {
        if (needToOffload()) {
            // TODO send to GS to offload
        } else {
            int monitorGridSchedulerId = selectMonitorGridSchedule();
            int backUpGridSchedulerId = selectBackUpGridSchedule(monitorGridSchedulerId);

            Optional<IGridScheduler> monitorGridScheduler = gridSchedulerRepository.getEntity(monitorGridSchedulerId);
            Optional<IGridScheduler> backUpGridScheduler = gridSchedulerRepository.getEntity(backUpGridSchedulerId);

            if(monitorGridScheduler.isPresent() && backUpGridScheduler.isPresent()) {
                // request monitoring and backup in parallel
                Task<Void> monitorTask = Task.action(() -> {
                    System.out.println("Requesting monitoring for " + req.getJob().getJobId());
                    MonitoringRequest monitoringRequest = new MonitoringRequest(id, req);
                    monitorGridScheduler.get().monitor(monitoringRequest);
                    monitoredBy.put(req, monitorGridSchedulerId);
                });

                Task<Void> backUpTask = Task.action(() -> {
                    System.out.println("Requesting back-up for " + req.getJob().getJobId());
                    BackUpRequest backUpRequest = new BackUpRequest(id, req);
                    backUpGridScheduler.get().backUp(backUpRequest);
                    backedUpAt.put(req, backUpGridSchedulerId);
                });

                Task<?> await = Task.par(monitorTask, backUpTask);
                engine.run(monitorTask);
                engine.run(backUpTask);
                engine.run(await); // needed?
                try {
                    await.await();

                    schedule(req);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RemoteException("Something went wrong requesting monitoring/backup. retry?");
                }
            } else {
                throw new RemoteException("no monitoring and backup grid scheduler available");
            }

        }
    }

    protected boolean needToOffload() {
        return false;
    }

    public int selectMonitorGridSchedule() {
        return gridSchedulerRepository.ids().get(0);
    }

    public int selectBackUpGridSchedule(int monitorGridScheduleId) {
        return gridSchedulerRepository.onlineIdsExcept(monitorGridScheduleId).get(0);
    }

    @Override
    public void schedule(@NotNull WorkRequest toSchedule) throws RemoteException {
        System.out.println("Scheduling job " + toSchedule.getJob().getJobId());
        jobQueue.offer(toSchedule);
        processQueue();
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
        int monitorGridSchedulerId = monitoredBy.get(req);
        int backUpGridSchedulerId = backedUpAt.get(req);
        Optional<IGridScheduler> monitorGridScheduler = gridSchedulerRepository.getEntity(monitorGridSchedulerId);
        Optional<IGridScheduler> backUpGridScheduler = gridSchedulerRepository.getEntity(backUpGridSchedulerId);

        if(monitorGridScheduler.isPresent() && backUpGridScheduler.isPresent()) {
            monitorGridScheduler.get().releaseMonitored(req);
            backUpGridScheduler.get().releaseBackUp(req);
        }
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
    public synchronized void start() throws RemoteException {
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new ArrayBlockingQueue<>(nNodes);

        monitoredBy = new HashMap<>();
        backedUpAt = new HashMap<>();

        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        this.timer = Executors.newSingleThreadScheduledExecutor();
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timer).build();


        for(int i = 0; i < nNodes; i++) {
            Node node = new Node(i, this, timer);
            idleNodes.add(node);
        }

        running = true;

        for (int gridSchedulerId : gridSchedulerRepository.ids()) {
            gridSchedulerRepository.getEntity(gridSchedulerId).ifPresent(gs -> {
                try {
                    gs.receiveResourceManagerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // Can be offline. that's okay.
                }
            });
        }

        gridSchedulerPinger.start();
    }

    @Override
    public synchronized void shutDown() throws RemoteException {
        jobQueue = null;
        idleNodes = null;

        monitoredBy = null;
        backedUpAt = null;

        timer.shutdownNow();
        engine.shutdown();
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        System.out.println("GS " + from + " awake");
        gridSchedulerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {

    }
}