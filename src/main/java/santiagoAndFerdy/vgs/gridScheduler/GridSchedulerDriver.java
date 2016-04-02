package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GridSchedulerDriver extends UnicastRemoteObject implements IGridSchedulerDriver {
    private static final long             serialVersionUID = -5694724140595312739L;

    private IRepository<IResourceManagerGridSchedulerClient> rmRepository;
    private IRepository<IGridSchedulerGridSchedulerClient> gsRepository;

    private Queue<MonitoringRequest> monitoredJobs;
    private Queue<BackUpRequest> backUpMonitoredJobs;

    private RmiServer rmiServer;
    private int id;
    private String url;

    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;
    
    public GridSchedulerDriver(RmiServer rmiServer,
                               IRepository<IResourceManagerGridSchedulerClient> rmRepository,
                               IRepository<IGridSchedulerGridSchedulerClient> gsRepository,
                               String url,
                               int id) throws RemoteException, MalformedURLException {
        this.rmiServer = rmiServer;
        this.url = url;
        this.id = id;

        this.rmRepository = rmRepository;
        this.gsRepository = gsRepository;

        monitoredJobs = new PriorityQueue<>();
        backUpMonitoredJobs = new PriorityQueue<>();

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();

        wakeUp();
    }

    @Override
    public void ping(@NotNull Heartbeat h) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException {
        //TODO ???
    }

    @Override
    public synchronized void monitorPrimary(MonitoringRequest request) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Received job " + request.getJobToMonitor().getJobId() + " to monitor ");
        monitoredJobs.add(request);
        IGridSchedulerGridSchedulerClient backUp = selectBackUp();

        // ACK
        IGridSchedulerResourceManagerClient sourceClient = (IGridSchedulerResourceManagerClient) Naming.lookup(request.getSourceClientUrl());
        sourceClient.monitoringRequestAccepted(request.getJobToMonitor());
    }

    public IGridSchedulerGridSchedulerClient selectBackUp() {
        // TODO

        return null;
    }

    @Override
    public void monitorBackUp(BackUpRequest backUpRequest) throws RemoteException, MalformedURLException, NotBoundException {
        backUpMonitoredJobs.add(backUpRequest);
        IGridSchedulerGridSchedulerClient client = (IGridSchedulerGridSchedulerClient)Naming.lookup(backUpRequest.getSourceUrl());
        client.monitorBackUpAccepted(backUpRequest.getJobToBackUp());
    }

    @Override
    public void offload(UserRequest userRequest) {

    }

    @Override
    public void releaseResources(int requestId) {

    }

    public void wakeUp() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);

        //TODO + broadcast wakeup!
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }
}