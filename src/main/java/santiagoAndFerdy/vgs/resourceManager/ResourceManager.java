package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.function.Function2;
import com.sun.istack.internal.NotNull;

import santiagoAndFerdy.vgs.discovery.Pinger;
import santiagoAndFerdy.vgs.discovery.Status;
import santiagoAndFerdy.vgs.discovery.selector.Selectors;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.*;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.user.IUser;

/**
 * Created by Fydio on 3/19/16.
 */
public class ResourceManager extends UnicastRemoteObject implements IResourceManager {
    private static final long              serialVersionUID = -5340659478612949546L;

    private RmiServer                      rmiServer;
    private int                            id;
    private IRepository<IUser>             userRepository;
    private IRepository<IResourceManager>  rmRepository;
    private IRepository<IGridScheduler>    gsRepository;
    private Pinger<IGridScheduler>         gridSchedulerPinger;

    private Queue<WorkRequest>             jobQueue;
    private Queue<Node>                    idleNodes;
    private int                            nNodes;
    private int                            queueSize;
    private Map<WorkRequest, Integer>      monitoredBy;
    private Map<WorkRequest, Integer>      backedUpBy;
    private Map<Integer, Set<WorkRequest>> monitoredAt;
    private Map<Integer, Set<WorkRequest>> backedUpAt;

    private Boolean                        recovering;

    private ScheduledExecutorService       timer;
    private Engine                         engine;

    private int                            load;

    private boolean                        running;

    public ResourceManager(RmiServer rmiServer, int id, IRepository<IUser> userRepository, // TODO use appendable user repository.
            IRepository<IResourceManager> rmRepository, IRepository<IGridScheduler> gsRepository, int nNodes) throws RemoteException {
        this.rmiServer = rmiServer;
        this.id = id;
        this.userRepository = userRepository;
        this.rmRepository = rmRepository;
        this.gsRepository = gsRepository;
        rmiServer.register(rmRepository.getUrl(id), this);
        gridSchedulerPinger = new Pinger(gsRepository);
        this.nNodes = nNodes;
        queueSize = nNodes;
        running = false;

        start();
        setUpMonitorCrashRecovery();
        setUpBackUpCrash();
    }

    @Override
    public synchronized void offerWork(WorkRequest req) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        if (needToOffload()) {
            gsRepository.invokeOnEntity((gs, gsId) -> {
                System.out.println("[RM\t" + id + "] Offloading to GS " + gsId + " job " + req.getJob().getJobId());
                gs.offLoad(req);
                return gsId;
            } , Selectors.invertedWeighedRandom);
        } else {
            Optional<?> setUpRedundancy = requestMonitoring(req).flatMap(monitorId -> {
                try {
                    return requestBackUp(req, monitorId);
                } catch (RemoteException e) {
                    return Optional.empty();
                }
            });

            if (setUpRedundancy.isPresent()) {
                schedule(req);
                engine.run(Task.action(() -> processQueue()));
            } else {
                throw new RemoteException("Failed to set up monitoring and backup.");
            }
        }
    }

    @Override
    public synchronized void orderWork(WorkOrder req) throws RemoteException {
        WorkRequest work = req.getWorkRequest();
        
        // removing possible old monitor reference
        Optional.ofNullable(monitoredBy.get(work)).ifPresent(oldMonitorId -> {
            monitoredAt.get(oldMonitorId).remove(work);
        });

        // update monitor admin
        monitoredBy.put(work, req.getFromGridSchedulerId());
        monitoredAt.get(req.getFromGridSchedulerId()).add(work);

        // admin the hop to here
        req.getWorkRequest().getJob().addResourceManagerId(id);
        Optional<?> backUp = requestBackUp(work, req.getFromGridSchedulerId());

        if (backUp.isPresent()) {
            System.out.println("[RM\t" + id + "] Received job from GS " + req.getFromGridSchedulerId());
            engine.run(Task.action(() -> schedule(work)));
        } else {
            throw new RemoteException("Failed to set up backup");
        }
    }

    protected boolean needToOffload() {
        return jobQueue.size() == queueSize;
    }

    protected Optional<Integer> requestMonitoring(WorkRequest req) throws RemoteException {

        return gsRepository.invokeOnEntity((gs, gsId) -> {
            System.out.println("[RM\t" + id + "] Requesting GS " + gsId + " to monitor job " + req.getJob().getJobId());
            gs.monitor(new MonitoringRequest(id, req));

            monitoredBy.put(req, gsId);
            monitoredAt.get(gsId).add(req);

            return gsId;
        } , Selectors.invertedWeighedRandom);
    }

    protected Optional<Integer> requestBackUp(WorkRequest req, int monitorId) throws RemoteException {
        return gsRepository.invokeOnEntity((gs, gsId) -> {
            System.out.println("[RM\t" + id + "] Requesting GS " + gsId + " to back up job " + req.getJob().getJobId());
            gs.backUp(new BackUpRequest(id, req));

            backedUpBy.put(req, gsId);
            backedUpAt.get(gsId).add(req);

            return gsId;
        } , Selectors.invertedWeighedRandom, monitorId);
    }

    protected synchronized void schedule(@NotNull WorkRequest toSchedule) throws RemoteException {
        System.out.println("[RM\t" + id + "] Scheduling job " + toSchedule.getJob().getJobId());
        jobQueue.offer(toSchedule);
        load += toSchedule.getJob().getDuration();
        engine.run(Task.action(() -> processQueue()));
    }

    @Override
    public synchronized void finish(Node node, WorkRequest finished) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        Optional<IUser> maybeUser = userRepository.getEntity(finished.getUserId());

        System.out.println("[RM\t" + id + "] Finished running job " + finished.getJob().getJobId());

        if (maybeUser.isPresent()) {
            maybeUser.get().acceptResult(finished.getJob());
            release(finished);
            idleNodes.offer(node);
            load -= finished.getJob().getDuration();
            processQueue();
        } else {
            System.err.println("User not present");
        }

    }

    public synchronized void release(WorkRequest req) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        int monitorGridSchedulerId = monitoredBy.get(req);
        int backUpGridSchedulerId = backedUpBy.get(req);
        Optional<IGridScheduler> monitorGridScheduler = gsRepository.getEntity(monitorGridSchedulerId);
        Optional<IGridScheduler> backUpGridScheduler = gsRepository.getEntity(backUpGridSchedulerId);

        monitorGridScheduler.ifPresent(monitor -> {
            try {
                monitor.releaseMonitored(req);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
        backUpGridScheduler.ifPresent(backUp -> {
            try {
                backUp.releaseBackUp(req);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    protected void processQueue() {
        while (true) {
            synchronized (idleNodes) {
                Node node = idleNodes.poll();
                if (node != null) {
                    synchronized (jobQueue) {
                        WorkRequest work = jobQueue.poll();
                        if (work != null) {
                            System.out.println("[RM\t" + id + "] Executing job " + work.getJob().getJobId());
                            node.handle(work);
                        } else {
                            idleNodes.offer(node);
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void setUpMonitorCrashRecovery() {
        gsRepository.onOffline(gsId -> {
            synchronized (recovering) {
                if (running && !recovering) {

                    recovering = true;
                    monitoredAt.remove(gsId).forEach(req -> {
                        // for each request at the crashed monitor
                        Optional<Integer> maybeBackUpId = Optional.ofNullable(backedUpBy.get(req));
                        maybeBackUpId.ifPresent(backUpId -> {
                            if (gsRepository.checkStatus(backUpId)) {
                                // back-up still alive
                                // promote back-up, request new back-up
                                gsRepository.getEntity(backUpId).ifPresent(gs -> {
                                    try {
                                        gs.promote(new PromotionRequest(id, req));

                                        monitoredBy.put(req, id);
                                        requestBackUp(req, backUpId);

                                    } catch (RemoteException e) {
                                        // warning
                                        System.out.println("[RM\t" + id + "] Can't fully fix redundancy for job " + req.getJob()
                                                .getJobId() + ". Try to finish anyway");
                                    }
                                });
                            } else {
                                // back-up down
                                // new backup, new monitor
                                try {
                                    requestMonitoring(req);
                                    requestBackUp(req, gsId);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        if (!maybeBackUpId.isPresent()) {
                            try {
                                requestBackUp(req, gsId);

                            } catch (RemoteException e) {
                                // warn
                                System.out.println("[RM\t" + id + "] Not enough GS available to make new back-up of job " + req.getJob().getJobId());
                            }
                        }
                    });

                    monitoredAt.get(gsId).clear();
                }
            }

            return null;
        });
    }

    public void setUpBackUpCrash() {
        gsRepository.onOffline(gsId -> {

            if (running && !recovering) {
                synchronized (recovering) {
                    recovering = true;
                }
                backedUpAt.remove(gsId).forEach(req -> {
                    Optional.ofNullable(monitoredBy.get(req)).ifPresent(monitorId -> {
                        if (gsRepository.checkStatus(monitorId)) {
                            // monitor still up, request new back-up
                            try {
                                requestBackUp(req, monitorId);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // monitor down, set up monitor and back-up
                            try {
                                requestMonitoring(req);
                                requestBackUp(req, monitorId);
                            } catch (RemoteException e) {
                                System.out.println("[RM\t" + id + "] Can't make new back-up for job " + req.getJob().getJobId());
                            }
                        }
                    });
                });

                synchronized (recovering) {
                    recovering = false;
                }
            }

            return null;
        });
    }

    @Override
    public synchronized void start() throws RemoteException {
        jobQueue = new ConcurrentLinkedQueue<>();
        idleNodes = new ArrayBlockingQueue<>(nNodes);

        monitoredBy = new HashMap<>();
        backedUpBy = new HashMap<>();
        monitoredAt = new HashMap<>();
        backedUpAt = new HashMap<>();

        recovering = false;

        gsRepository.ids().forEach(gsId -> {
            monitoredAt.put(gsId, new HashSet<>());
            backedUpAt.put(gsId, new HashSet<>());
        });

        load = 0;

        ExecutorService taskScheduler = Executors.newFixedThreadPool(100);
        this.timer = Executors.newSingleThreadScheduledExecutor();
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timer).build();

        for (int i = 0; i < nNodes; i++) {
            Node node = new Node(i, this, timer);
            idleNodes.add(node);
        }

        running = true;

        for (int gridSchedulerId : gsRepository.ids()) {
            gsRepository.getEntity(gridSchedulerId).ifPresent(gs -> {
                try {
                    gs.receiveResourceManagerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // Can be offline. that's okay.
                }
            });
        }

        gridSchedulerPinger.start();

        System.out.println("[RM\t" + id + "] Online");
    }

    @Override
    public synchronized void shutDown() throws RemoteException {
        running = false;

        jobQueue = null;
        idleNodes = null;

        monitoredBy = null;
        backedUpBy = null;
        monitoredAt = null;
        backedUpAt = null;

        timer.shutdownNow();
        engine.shutdown();

        System.out.println("[RM\t" + id + "] Offline");
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[RM\t" + id + "] GS " + from + " awake");
        gsRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        return id;
    }

    @Override
    public long ping() throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        return load;
    }
}