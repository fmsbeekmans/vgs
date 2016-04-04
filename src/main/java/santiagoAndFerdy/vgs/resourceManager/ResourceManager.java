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
import java.util.*;
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
    private Map<WorkRequest, Integer> backedUpBy;
    private Map<Integer, Set<WorkRequest>> monitoredAt;
    private Map<Integer, Set<WorkRequest>> backedUpAt;

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

        gridSchedulerRepository.onOffline(gsId -> {
            synchronized (monitoredAt) {
                Set<WorkRequest> needNewPrimary = monitoredAt.get(gsId);
                monitoredAt.put(gsId, new HashSet());

                needNewPrimary.forEach(req -> {
                    int backUpId = backedUpBy.get(req);
                    Optional<IGridScheduler> newMonitor = gridSchedulerRepository.getEntity(backUpId);

                    if(newMonitor.isPresent()) {
                        try {
                            newMonitor.get().promote(req);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            // TODO: Error
                        }
                    } else {
                        // TODO: Error, find yet another?
                    }
                });
            }

            return null;
        });

        this.nNodes = nNodes;
        running = false;

        start();
    }

    @Override
    public void offerWork(WorkRequest req) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        engine.run(Task.action(() -> {
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
                        monitoredAt.get(monitorGridSchedulerId).add(req);
                    });

                    Task<Void> backUpTask = Task.action(() -> {
                        System.out.println("Requesting back-up for " + req.getJob().getJobId());
                        BackUpRequest backUpRequest = new BackUpRequest(id, req);
                        backUpGridScheduler.get().backUp(backUpRequest);
                        backedUpBy.put(req, backUpGridSchedulerId);
                        backedUpAt.get(backUpGridSchedulerId).add(req);
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
        }));
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

    protected void schedule(@NotNull WorkRequest toSchedule) throws RemoteException {
        System.out.println("Scheduling job " + toSchedule.getJob().getJobId());
        jobQueue.offer(toSchedule);
        engine.run(Task.action(() -> processQueue()));
    }

    @Override
    public synchronized void finish(Node node, WorkRequest finished) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        Optional<IUser> maybeUser = userRepository.getEntity(finished.getUserId());

        System.out.println("Finished running job " + finished.getJob().getJobId());

        if(maybeUser.isPresent()) {
            maybeUser.get().acceptResult(finished.getJob());
            release(finished);
            idleNodes.offer(node);
            processQueue();
        } else {
            // TODO What to do when the user isn't there to accept the result?
        }

    }

    public synchronized void release(WorkRequest req) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        int monitorGridSchedulerId = monitoredBy.get(req);
        int backUpGridSchedulerId = backedUpBy.get(req);
        Optional<IGridScheduler> monitorGridScheduler = gridSchedulerRepository.getEntity(monitorGridSchedulerId);
        Optional<IGridScheduler> backUpGridScheduler = gridSchedulerRepository.getEntity(backUpGridSchedulerId);

        if(monitorGridScheduler.isPresent() && backUpGridScheduler.isPresent()) {
            monitorGridScheduler.get().releaseMonitored(req);
            backUpGridScheduler.get().releaseBackUp(req);
        }
    }

    protected synchronized void processQueue() {
        while (true) {
            Node node = idleNodes.poll();
            if(node != null) {
                WorkRequest work = jobQueue.poll();
                if(work != null) {
                    System.out.println("Executing job " + work.getJob().getJobId());
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
        backedUpBy = new HashMap<>();
        monitoredAt = new HashMap<>();
        backedUpAt = new HashMap<>();

        gridSchedulerRepository.ids().forEach(gsId -> {
            monitoredAt.put(gsId, new HashSet<>());
            backedUpAt.put(gsId, new HashSet<>());
        });

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
        backedUpBy = null;
        monitoredAt = null;
        backedUpAt = null;

        timer.shutdownNow();
        engine.shutdown();
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        System.out.println("GS " + from + " awake");
        gridSchedulerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        return id;
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");


    }
}