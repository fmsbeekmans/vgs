package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler, IRemoteShutdown {
    private static final long                                serialVersionUID = -5694724140595312739L;

    private IRepository<IResourceManager> resourceManagerRepository;
    private IRepository<IGridScheduler> gridSchedulerRepository;

    private Queue<MonitoringRequest>                         monitoredJobs;
    private Queue<BackUpRequest>                             backUpMonitoredJobs;

    private RmiServer                                        rmiServer;
    private int                                              id;
    private String                                           url;

    private HeartbeatHandler                                 heartbeatHandler;
    private ScheduledExecutorService                         timerScheduler;
    private Engine                                           engine;

    public GridScheduler(RmiServer rmiServer,
                         IRepository<IResourceManager> rmRepository,
                         IRepository<IGridScheduler> gsRepository,
                         String url, int id) throws RemoteException, MalformedURLException {
        this.rmiServer = rmiServer;
        this.url = url;
        this.id = id;

        this.resourceManagerRepository = rmRepository;
        this.gridSchedulerRepository = gsRepository;

        monitoredJobs = new PriorityQueue<>();
        backUpMonitoredJobs = new PriorityQueue<>();

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        heartbeatHandler = new HeartbeatHandler(rmRepository, id, false);
        wakeUp();
    }

    @Override
    public synchronized void monitorPrimary(MonitoringRequest request) throws RemoteException {
        System.out.println("Received job " + request.getToMonitor().getJob().getJobId() + " to monitor ");
        monitoredJobs.add(request);
        Optional<IGridScheduler> backUpTarget = selectBackUp();
        if(backUpTarget.isPresent()) {
            BackUpRequest backUpRequest = new BackUpRequest(id, request.getToMonitor());
            backUpTarget.get().monitorBackUp(backUpRequest);
        } else {
            // TODO error handling. Can't reach backup gs.
        }

    }

    public Optional<IGridScheduler> selectBackUp() {
        return gridSchedulerRepository.getEntity(0);
    }

    @Override
    public void monitorBackUp(BackUpRequest backUpRequest) throws RemoteException {
        backUpMonitoredJobs.add(backUpRequest);
    }

    @Override
    public void offload(Job userRequest) {

    }

    @Override
    public void releaseResources(int requestId) {

    }

    public void wakeUp() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);

        // TODO + broadcast wakeup!
    }

    public void checkConnections() {
        heartbeatHandler.getStatus();
    }

    @Override
    public void iAmAlive(Heartbeat h) throws RemoteException {

    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    /**
     * This method is called by the Simulation Launcher to kill the node. However it is also needed to kill the process, so we will have to implement
     * a mechanisim to kill all the nodes in a clean way, probably with Threads
     */
    @Override
    public void shutDown() {
        try {
            rmiServer.unRegister(url);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}