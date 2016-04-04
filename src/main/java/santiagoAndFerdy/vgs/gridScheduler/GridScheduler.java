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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.PriorityQueue;
import java.util.Queue;

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler {
    private static final long             serialVersionUID = -5694724140595312739L;

    private RmiServer                     rmiServer;
    private int                           id;
    private IRepository<IResourceManager> resourceManagerRepository;
    private IRepository<IGridScheduler>   gridSchedulerRepository;
    private Pinger<IResourceManager> resourceManagerPinger;
    private Pinger<IGridScheduler> gridSchedulerPinger;


    private Queue<MonitoringRequest>      monitoredJobs;
    private Queue<BackUpRequest>          backUpMonitoredJobs;

    private boolean running;

    public GridScheduler(
            RmiServer rmiServer,
            int id,
            IRepository<IResourceManager> resourceManagerRepository,
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
        if(!running) throw new RemoteException("I am offline");

        System.out.println("Received job " + monitorRequest.getToMonitor().getJob().getJobId() + " to monitor at cluster " + id);
        monitoredJobs.add(monitorRequest);
    }

    @Override
    public void backUp(BackUpRequest backUpRequest) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        System.out.println("Received backup request from " + backUpRequest.getSourceGridSchedulerId() + " at cluster " + id);
        backUpMonitoredJobs.add(backUpRequest);
    }

    @Override
    public void offLoad(WorkRequest workRequest) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");


    }

    @Override
    public void releaseMonitored(WorkRequest request) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        System.out.println("Stop monitoring " + request.getJob().getJobId() + " at cluster " + id);
        monitoredJobs.remove(request);
    }

    @Override
    public void releaseBackUp(WorkRequest workRequest) throws RemoteException {
        if(!running) throw new RemoteException("I am offline");

        System.out.println("Releasing back-up of workRequest " + workRequest.getJob().getJobId() + " at cluster " + id);
        backUpMonitoredJobs.remove(workRequest);
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {

    }

    @Override
    public void start() throws RemoteException {
        running = true;
        monitoredJobs = new PriorityQueue<>();
        backUpMonitoredJobs = new PriorityQueue<>();

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

        for(int resourceManagerId : resourceManagerRepository.ids()) {
            resourceManagerRepository.getEntity(resourceManagerId).ifPresent(rm -> {
                try {
                    rm.receiveGridSchedulerWakeUpAnnouncement(id);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }

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
        System.out.println("RM " + from + " awake");
        resourceManagerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        System.out.println("GS " + from + " awake");
        gridSchedulerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() {
        return id;
    }
}