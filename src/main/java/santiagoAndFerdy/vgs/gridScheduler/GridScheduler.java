package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Pinger;
import santiagoAndFerdy.vgs.discovery.Status;
import santiagoAndFerdy.vgs.discovery.selector.Selectors;
import santiagoAndFerdy.vgs.messages.*;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler {
    private static final long              serialVersionUID = -5694724140595312739L;

    private RmiServer                      rmiServer;
    private int                            id;
    private IRepository<IResourceManager>  rmRepository;
    private IRepository<IGridScheduler>    gsRepository;
    private Pinger<IResourceManager>       resourceManagerPinger;
    private Pinger<IGridScheduler>         gridSchedulerPinger;

    private Map<Integer, Set<WorkRequest>> monitoredJobs;
    private Map<WorkRequest, MonitorRequest> pendingMonitoringRequests;
    private Map<Integer, Set<BackUpRequest>> backUpJobs;
    private Map<WorkRequest, Integer> pendingBackUpRequests;

    private Map<WorkRequest, List<Integer>> backUpPathsBefore;
    private Map<WorkRequest, List<Integer>> backUpPathsAfter;

    private ScheduledExecutorService timer;
    private Engine engine;

    private boolean                        running;

    private long                           load;

    public GridScheduler(RmiServer rmiServer, int id, IRepository<IResourceManager> rmRepository, IRepository<IGridScheduler> gsRepository)
            throws RemoteException {
        this.rmiServer = rmiServer;
        this.id = id;
        rmiServer.register(gsRepository.getUrl(id), this);

        this.rmRepository = rmRepository;
        this.gsRepository = gsRepository;
        gridSchedulerPinger = new Pinger(gsRepository);
        resourceManagerPinger = new Pinger(rmRepository);

        start();
    }

    @Override
    public void monitor(MonitorRequest monitorRequest) throws RemoteException {
        if (!running) throw new RemoteException("I am offline");

        engine.run(Task.action(() -> {
            WorkRequest work = monitorRequest.getWorkRequest();
            System.out.println("[GS\t" + id + "] Received job " + work.getJob().getJobId() + " to monitor at GS " + id);

            monitoredJobs.get(monitorRequest.getSourceResourceManagerId()).add(work);

            BackUpRequest backUpRequest = new BackUpRequest(work, id, 2);
            gsRepository.invokeOnEntity((gs, gsId) -> {
                try {
                    System.out.println("[GS\t" + id + "] Sending backup monitoring request to GS " + gsId + " for job " + work.getJob().getJobId());
                    pendingMonitoringRequests.put(work, monitorRequest);
                    gs.backUp(backUpRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }, Selectors.invertedWeighedRandom, id);
        }));
    }

    @Override
    public void backUp(BackUpRequest backUpRequest) throws RemoteException {
        if (!running) throw new RemoteException("I am offline");

        engine.run(Task.action(() -> {
            WorkRequest work = backUpRequest.getWorkRequest();

            List<Integer> trailInit = new LinkedList<>();
            Arrays.stream(backUpRequest.getTrail()).forEach(trailInit::add);
            backUpPathsBefore.put(work, trailInit);

            if(backUpRequest.getBackUpsRequested() == 0) {
                // I'm the last, send reply directly
                BackUpAck ack = new BackUpAck(work, id);

                int ackToId = backUpRequest.getTrail()[backUpRequest.getTrail().length - 1];
                Optional<IGridScheduler> ackTo = gsRepository.getEntity(ackToId);

                if(ackTo.isPresent()) {
                    ackTo.ifPresent(gs -> {
                        try {
                            System.out.println("[GS\t" + id + "] Received backup request from with trail " + Arrays.toString(backUpRequest.getTrail()) + " and " + backUpRequest.getBackUpsRequested());
                            gs.acceptBackUpAck(ack);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                } else {

                }
            } else {
                // Make more backups
                BackUpRequest newBackUpRequest = backUpRequest.hop(id);
                Optional<BackUpAck> ack =  gsRepository.invokeOnEntity((gs, gsId) -> {
                    System.out.println("[GS\t" + id + "] Sending job " + work.getJob().getJobId() + " to GS " + id + " to make more backups.");
                    pendingBackUpRequests.put(work, backUpRequest.getTrail()[backUpRequest.getTrail().length - 1]);

                    gs.backUp(newBackUpRequest);
                    // TODO dubble check

                    return null;
                }, Selectors.invertedWeighedRandom, newBackUpRequest.getTrail());
            }
        }));
    }

    @Override
    public void acceptBackUpAck(BackUpAck ack) throws RemoteException {
        if (!running) throw new RemoteException("I am offline");

        engine.run(Task.action(() -> {
            WorkRequest work = ack.getWorkRequest();

            // path admin
            List<Integer> after = new LinkedList<Integer>();
            Arrays.stream(ack.getBackUps()).forEach(after::add);
            backUpPathsAfter.put(work, after);

            System.out.println("[GS\t" + id + "] Backup path for job " + work.getJob().getJobId() + " path is " + Arrays.toString(after.toArray()) + " to RM");
            Optional<MonitorRequest> maybeMonitorRequest = Optional.ofNullable(pendingMonitoringRequests.remove(ack.getWorkRequest()));
            if (maybeMonitorRequest.isPresent()) {
                maybeMonitorRequest.ifPresent(monitorRequest -> {
                    Optional<IResourceManager> maybeRm = rmRepository.getEntity(monitorRequest.getSourceResourceManagerId());

                    if (maybeRm.isPresent()) {
                        try {
                            maybeRm.get().monitorAck(ack.prependGridSchedulerList(id));
                        } catch (RemoteException e) {
                            // TODO Reschedule
                            e.printStackTrace();
                        }
                    } else {
                        // TODO Reschedule
                    }
                });
            } else {
                Optional<Integer> maybeGsToAckId = Optional.ofNullable(pendingBackUpRequests.remove(ack.getWorkRequest()));

                if (maybeGsToAckId.isPresent()) {
                    System.out.println("[GS\t" + id + "] Propagate backup ack for job " + work.getJob().getJobId() + " from " + Arrays.toString(ack.getBackUps()));
                    maybeGsToAckId.flatMap(gsRepository::getEntity).ifPresent(gsToAck -> {
                        try {
                            gsToAck.acceptBackUpAck(ack.prependGridSchedulerList(id));

                        } catch (RemoteException e) {
                            // can't be reached? Then do nothing. first part of the chain will do recovery.
                            e.printStackTrace();
                        }
                    });
                }
            }
        }));
    }

    @Override
    public void promote(PromotionRequest promotionRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS\t" + id + "] Promoting to primary for job " + promotionRequest.getToBecomePrimaryFor().getJob()
                .getJobId() + " at cluster " + id);
        backUpJobs.get(promotionRequest.getSourceResourceManagerId()).remove(promotionRequest.getToBecomePrimaryFor());
        monitoredJobs.get(promotionRequest.getSourceResourceManagerId()).add(promotionRequest.getToBecomePrimaryFor());

    }

    @Override
    public void offLoad(WorkRequest workRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

    }

    @Override
    public void releaseMonitored(WorkRequest request) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        monitoredJobs.remove(request);
        System.out.println("[GS\t" + id + "] Stop monitoring " + request.getJob().getJobId() + " at cluster " + id);
    }

    @Override
    public void releaseBackUp(WorkRequest workRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        backUpJobs.remove(workRequest);
        System.out.println("[GS\t" + id + "] Releasing back-up of workRequest " + workRequest.getJob().getJobId() + " at cluster " + id);
    }

    @Override
    public void start() throws RemoteException {
        running = true;
        monitoredJobs = new HashMap<>();
        pendingMonitoringRequests = new HashMap<>();
        backUpJobs = new HashMap<>();
        pendingBackUpRequests = new HashMap<>();

        backUpPathsBefore = new HashMap<>();
        backUpPathsAfter = new HashMap<>();

        for (int rmId : rmRepository.ids()) {
            monitoredJobs.put(rmId, new HashSet<>());
            backUpJobs.put(rmId, new HashSet<>());
            rmRepository.getEntity(rmId).ifPresent(rm -> {
                try {
                    rm.receiveGridSchedulerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                }
            });
        }

        for (int gsId : gsRepository.idsExcept(id)) {
            gsRepository.getEntity(gsId).ifPresent(gs -> {
                try {
                    gs.receiveGridSchedulerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    // Can be offline. that's okay.
                }
            });
        }

        load = 1;

        ExecutorService taskScheduler = Executors.newFixedThreadPool(100);
        this.timer = Executors.newSingleThreadScheduledExecutor();
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timer).build();

        gridSchedulerPinger.start();
        resourceManagerPinger.start();

        System.out.println("[GS\t" + id + "] Online");
    }

    @Override
    public void shutDown() throws RemoteException {
        running = false;
        monitoredJobs = null;
        backUpJobs = null;
        pendingMonitoringRequests = null;
        pendingBackUpRequests = null;

        backUpPathsBefore = null;
        backUpPathsAfter = null;

        gridSchedulerPinger.stop();
        resourceManagerPinger.stop();

        System.out.println("[GS\t" + id + "] Offline");
    }

    @Override
    public void receiveResourceManagerWakeUpAnnouncement(int from) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS\t" + id + "] RM " + from + " awake");
        rmRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS\t" + id + "] GS " + from + " awake");
        gsRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public long ping() throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        return load;
    }

    @Override
    public int getId() throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        return id;
    }
}