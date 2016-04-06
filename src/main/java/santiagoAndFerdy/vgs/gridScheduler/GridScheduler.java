package santiagoAndFerdy.vgs.gridScheduler;

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

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler {
    private static final long              serialVersionUID = -5694724140595312739L;

    private RmiServer                      rmiServer;
    private int                            id;
    private IRepository<IResourceManager>  rmRepository;
    private IRepository<IGridScheduler>    gsRepository;
    private Pinger<IResourceManager>       resourceManagerPinger;
    private Pinger<IGridScheduler>         gridSchedulerPinger;

    private Map<Integer, Set<WorkRequest>> monitoredJobs;
    private Map<Integer, Set<WorkRequest>> backUpJobs;

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
        setUpReSchedule();
        setUpSelfPromote();
    }

    @Override
    public synchronized void monitor(MonitoringRequest monitorRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS\t" + id + "] Received job " + monitorRequest.getToMonitor().getJob().getJobId() + " to monitor at cluster " + id);
        monitoredJobs.get(monitorRequest.getSourceResourceManagerId()).add(monitorRequest.getToMonitor());
    }

    @Override
    public void backUp(BackUpRequest backUpRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS\t" + id + "] Received backup request from " + backUpRequest.getSourceResourceManagerId() + " at cluster " + id);
        backUpJobs.get(backUpRequest.getSourceResourceManagerId()).add(backUpRequest.getToBackUp());
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
        backUpJobs = new HashMap<>();

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

        gridSchedulerPinger.start();
        resourceManagerPinger.start();

        System.out.println("[GS\t" + id + "] Online");
    }

    @Override
    public void shutDown() throws RemoteException {
        running = false;
        monitoredJobs = null;
        backUpJobs = null;

        gridSchedulerPinger.stop();
        resourceManagerPinger.stop();

        System.out.println("[GS\t" + id + "] Offline");
    }

    public void setUpReSchedule() {
        rmRepository.onOffline(rmId -> {
            if (running) {
                monitoredJobs.get(rmId).forEach(monitored -> {
                    System.out.println("[GS\t" + id + "] Rescheduling job " + monitored.getJob().getJobId());
                    WorkOrder reScheduleOrder = new WorkOrder(id, monitored);

                    Map<Integer, Long> loads = rmRepository.getLastKnownLoads();
                    Optional<IResourceManager> newRm = Selectors.weighedRandom.getRandomIndex(loads)
                            .flatMap(newRmId -> rmRepository.getEntity(newRmId));
                    newRm.ifPresent(rm -> {
                        try {
                            rm.orderWork(reScheduleOrder);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });

                });
            }

            return null;
        });
    }

    public void setUpSelfPromote() {

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