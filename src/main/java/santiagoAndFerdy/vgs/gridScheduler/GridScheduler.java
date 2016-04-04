package santiagoAndFerdy.vgs.gridScheduler;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Pinger;
import santiagoAndFerdy.vgs.discovery.Status;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import sun.swing.BakedArrayList;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler {
    private static final long                      serialVersionUID = -5694724140595312739L;

    private RmiServer                              rmiServer;
    private int                                    id;
    private IRepository<IResourceManager>          resourceManagerRepository;
    private IRepository<IGridScheduler>            gridSchedulerRepository;
    private Pinger<IResourceManager>               resourceManagerPinger;
    private Pinger<IGridScheduler>                 gridSchedulerPinger;
    private Map<Integer, Queue<MonitoringRequest>> monitoredJobs;

    private Map<Integer, Queue<WorkRequest>>       backUpMonitoredJobs;

    private boolean                                running;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GridScheduler(RmiServer rmiServer, int id, IRepository<IResourceManager> resourceManagerRepository,
            IRepository<IGridScheduler> gridSchedulerRepository) throws RemoteException {
        this.rmiServer = rmiServer;
        this.id = id;
        rmiServer.register(gridSchedulerRepository.getUrl(id), this);

        this.resourceManagerRepository = resourceManagerRepository;
        this.gridSchedulerRepository = gridSchedulerRepository;
        gridSchedulerPinger = new Pinger(gridSchedulerRepository, id);
        resourceManagerPinger = new Pinger(resourceManagerRepository);

        start();
    }

    @Override
    public synchronized void monitor(MonitoringRequest monitorRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        System.out.println("[GS" + id + "] Received job " + monitorRequest.getToMonitor().getJob().getJobId() + " to monitor at cluster " + id);
        int idRm = monitorRequest.getSourceResourceManagerId();
        Queue<MonitoringRequest> queue;
        if (monitoredJobs.containsKey(idRm))
            queue = monitoredJobs.get(idRm);
        else
            queue = new PriorityQueue<MonitoringRequest>();
        queue.add(monitorRequest);
        monitoredJobs.put(idRm, queue);
    }

    @Override
    public void backUp(BackUpRequest backUpRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        System.out.println("[GS" + id + "] Received backup request from " + backUpRequest.getSourceResourceManagerId() + " at cluster " + id);
        int idRm = backUpRequest.getSourceResourceManagerId();
        Queue<WorkRequest> queue;
        if (backUpMonitoredJobs.containsKey(idRm))
            queue = backUpMonitoredJobs.get(idRm);
        else
            queue = new PriorityQueue<WorkRequest>();
        queue.add(backUpRequest.getToBackUp());
        backUpMonitoredJobs.put(idRm, queue);
    }

    @Override
    public void promote(MonitoringRequest req) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        System.out.println("[GS" + id + "] Promoting to primary for job " + req.getToMonitor().getJob() + " at cluster " + id);
        backUpMonitoredJobs.remove(req.getToMonitor());
        int idRm = req.getToMonitor().getJob().getCurrentResourceManagerId();
        Queue<MonitoringRequest> queue;
        if (monitoredJobs.containsKey(idRm))
            queue = monitoredJobs.get(idRm);
        else
            queue = new PriorityQueue<MonitoringRequest>();
        queue.add(req);
        monitoredJobs.put(idRm, queue);
        Optional<IGridScheduler> mayBeGs = gridSchedulerRepository.getEntity(req.getBackUpMonitored());
        if (mayBeGs.isPresent()) {
            mayBeGs.get().backUp(new BackUpRequest(idRm, req.getToMonitor()));
        } else {
            // TODO ERROR
        }

    }

    @Override
    public void offLoad(WorkRequest workRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

    }

    @Override
    public void releaseMonitored(WorkRequest request) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        monitoredJobs.remove(request);
        System.out.println("[GS" + id + "] Stop monitoring " + request.getJob().getJobId() + " at cluster " + id);
    }

    @Override
    public void releaseBackUp(WorkRequest workRequest) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        backUpMonitoredJobs.remove(workRequest);
        System.out.println("[GS" + id + "] Releasing back-up of workRequest " + workRequest.getJob().getJobId() + " at cluster " + id);
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");
    }

    @Override
    public void start() throws RemoteException {
        running = true;
        monitoredJobs = new HashMap<>();
        backUpMonitoredJobs = new HashMap<>();

        for (int gridSchedulerId : gridSchedulerRepository.idsExcept(id)) {
            gridSchedulerRepository.getEntity(gridSchedulerId).ifPresent(gs -> {
                try {
                    gs.receiveGridSchedulerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    // Can be offline. that's okay.
                }
            });
        }

        for (int resourceManagerId : resourceManagerRepository.ids()) {
            resourceManagerRepository.getEntity(resourceManagerId).ifPresent(rm -> {
                try {
                    rm.receiveGridSchedulerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
        resourceManagerRepository.onOffline(rmId -> {
            synchronized (monitoredJobs) {
                Queue<MonitoringRequest> needNewRM = monitoredJobs.get(rmId);

                needNewRM.forEach(req -> {

                    Optional<IResourceManager> newRM = resourceManagerRepository.getEntityExceptId(rmId);

                    if (newRM.isPresent()) {
                        try { // Update the job status
                            req.getToMonitor().getJob().addResourceManagerId(newRM.get().getId());
                            // Notify the back up
                            Optional<IGridScheduler> mayBeGs = gridSchedulerRepository.getEntity(req.getBackUpMonitored());
                            if (mayBeGs.isPresent()) {
                                mayBeGs.get().modificateBackUp(rmId, newRM.get().getId());
                            }
                        } catch (Exception e1) {
                            // TODO Error
                            e1.printStackTrace();
                        }
                    } else {
                        // TODO: Error
                    }
                });
            }
            return null;
        });

        gridSchedulerPinger.start();
        resourceManagerPinger.start();
    }

    @Override
    public void shutDown() throws RemoteException {
        running = false;
        monitoredJobs = null;
        backUpMonitoredJobs = null;

        gridSchedulerPinger.stop();
        resourceManagerPinger.stop();
    }

    @Override
    public void receiveResourceManagerWakeUpAnnouncement(int from) throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        System.out.println("[GS" + id + "] RM " + from + " awake");
        resourceManagerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        if (!running)
            throw new RemoteException("I am offline");

        System.out.println("[GS" + id + "] GS " + from + " awake");
        gridSchedulerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() throws RemoteException {
        if (!running)
            throw new RemoteException("[GS" + id + "] I am offline");

        return id;
    }

    @Override
    public void modificateBackUp(int origiRm, int newRm) throws RemoteException {
        Queue<WorkRequest> queue = backUpMonitoredJobs.remove(origiRm);
        if (queue != null) {
            queue.forEach(req -> {
                req.getJob().addResourceManagerId(newRm);
            });
            if (backUpMonitoredJobs.containsKey(newRm)) {
                Queue<WorkRequest> presentQueue = backUpMonitoredJobs.get(newRm);
                presentQueue.addAll(queue);
                backUpMonitoredJobs.put(newRm, presentQueue);
            } else {
                backUpMonitoredJobs.put(newRm, queue);
            }
        }

    }
}