package santiagoAndFerdy.vgs.gridScheduler;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Status;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;
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

        // TODO annouche wakeup
    }

    @Override
    public void shutDown() throws RemoteException {
        running = false;
        monitoredJobs = null;
        backUpMonitoredJobs = null;
    }

    @Override
    public void receiveResourceManagerWakeUpAnnouncement(int from) throws RemoteException {
        resourceManagerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public void receiveGridSchedulerWakeUpAnnouncement(int from) throws RemoteException {
        gridSchedulerRepository.setLastKnownStatus(from, Status.ONLINE);
    }

    @Override
    public int getId() {
        return id;
    }
}