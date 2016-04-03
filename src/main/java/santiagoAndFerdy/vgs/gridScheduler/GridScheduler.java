package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GridScheduler extends UnicastRemoteObject implements IGridScheduler, IRemoteShutdown, Runnable {
    private static final long             serialVersionUID = -5694724140595312739L;

    private IRepository<IResourceManager> resourceManagerRepository;
    private IRepository<IGridScheduler>   gridSchedulerRepository;

    private Queue<MonitoringRequest>      monitoredJobs;
    private Queue<BackUpRequest>          backUpMonitoredJobs;

    private RmiServer                     rmiServer;
    private int                           id;
    private String                        url;
    private HeartbeatHandler              heartbeatHandlerGS;
    private HeartbeatHandler              heartbeatHandlerRM;
    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;
    private boolean running;
    private Thread pollingThread;
    public GridScheduler(RmiServer rmiServer, IRepository<IResourceManager> rmRepository, IRepository<IGridScheduler> gsRepository, String url,
            int id) throws RemoteException, MalformedURLException {
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
        heartbeatHandlerRM = new HeartbeatHandler(resourceManagerRepository, id, false);
        heartbeatHandlerGS = new HeartbeatHandler(gridSchedulerRepository, id, true);
        wakeUp();
    }

    @Override
    public synchronized void monitorPrimary(MonitoringRequest request) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Received job " + request.getToMonitor().getJob().getJobId() + " to monitor at cluster " + id);
        monitoredJobs.add(request);
    }

    @Override
    public void monitorBackUp(BackUpRequest backUpRequest) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Received backup request from: RM" + backUpRequest.getResourceManagerId() + " at cluster " + id);
        backUpMonitoredJobs.add(backUpRequest);
    }

    @Override
    public void offload(Job userRequest) {

    }

    @Override
    public void releaseResources(MonitoringRequest request) {
        if (!this.monitoredJobs.remove(request))
            System.err.println("There was an error releasing resources in cluster " + id);
        else
            System.out.println("Resources released for job " + request.getToMonitor().getJob().getJobId() + "at cluster " + id);
    }

    public void wakeUp() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);
        pollingThread = new Thread(this, "GS"+id);
        running = true;
        pollingThread.start();
        // TODO + broadcast wakeup!
    }

    public void checkConnections() {
        heartbeatHandlerRM.getStatus();
        heartbeatHandlerGS.getStatus();
    }

    @Override
    public void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException {

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
            running = false;
            pollingThread.join();
         
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void releaseBackUp(BackUpRequest backUpRequest) throws RemoteException {
        if (!this.backUpMonitoredJobs.remove(backUpRequest))
            System.err.println("There was an error releasing backup resources");
        else
            System.out.println("Released backup resources for job " + backUpRequest.getToBackUp().getJob().getJobId() + "at cluster " + id);

    }

    @Override
    public void run() {
       while(running){
           
       }
        
    }
}