package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;
import santiagoAndFerdy.vgs.resourceManager.ResourceManagerGridScheduleClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

public class GridSchedulerDriver extends UnicastRemoteObject implements IGridSchedulerDriver {
    private static final long             serialVersionUID = -5694724140595312739L;

    private IRepository<IResourceManagerGridSchedulerClient> rmRepository;
    private IRepository<IGridSchedulerGridSchedulerClient> gsRepository;
    private HashMap<Integer, HeartbeatHandler> rmHeartbeatHandlers;
    private HashMap<Integer, HeartbeatHandler> gsHeartbeatHandlers;

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
    public void monitorPrimary(Request request) throws RemoteException {

    }

    @Override
    public void manitorBackUp(Request request) throws RemoteException {

    }

    @Override
    public void promoteToPrimary(Request request) {

    }

    @Override
    public void offload(Request request) {

    }

    @Override
    public void releaseResources(int requestId) {

    }

    public void wakeUp() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);

        //TODO + broadcast wakeup!
    }
}