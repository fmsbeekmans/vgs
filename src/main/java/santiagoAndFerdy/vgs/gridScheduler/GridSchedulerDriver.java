package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.model.BackUpRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.model.MonitorRequest;
import santiagoAndFerdy.vgs.model.UserRequest;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GridSchedulerDriver extends UnicastRemoteObject implements IGridSchedulerDriver {
    private static final long             serialVersionUID = -5694724140595312739L;

    private IRepository<IResourceManagerGridSchedulerClient> rmRepository;
    private IRepository<IGridSchedulerGridSchedulerClient> gsRepository;
    private HashMap<Integer, HeartbeatHandler> rmHeartbeatHandlers;
    private HashMap<Integer, HeartbeatHandler> gsHeartbeatHandlers;

    private Queue<MonitorRequest> jobsPrimary;
    private Queue<BackUpRequest> jobsBackUp;

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
        this.rmHeartbeatHandlers = new HashMap<>();
        this.gsRepository = gsRepository;
        this.gsHeartbeatHandlers = new HashMap<>();

        jobsPrimary = new PriorityQueue<>();
        jobsBackUp = new PriorityQueue<>();

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
    public synchronized void monitorPrimary(MonitorRequest request) throws RemoteException {
        jobsPrimary.add(request);
        IGridSchedulerGridSchedulerClient backUp = selectBackUp();

        // do async
        engine.run(Task.action(() -> {
            // first send to back up
            BackUpRequest backUpRequest = new BackUpRequest(backUp, request.getJobToBackUp());
            Promise<Void> backUpAck = backUp.monitorBackUp(backUpRequest);
            backUpAck.addListener(backUpResult -> {
                    // TODO retry on different gs upon failure
                    request.getSource().jobBackUpped(request.getJobToBackUp());
            });
        }));
    }

    public IGridSchedulerGridSchedulerClient selectBackUp() {
        // TODO

        return null;
    }

    @Override
    public void monitorBackUp(BackUpRequest userRequest) throws RemoteException {

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
}