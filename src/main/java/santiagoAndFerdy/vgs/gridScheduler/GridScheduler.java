package santiagoAndFerdy.vgs.gridScheduler;

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

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;

import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class GridScheduler extends UnicastRemoteObject implements IHeartbeatReceiver, IRemoteShutdown {
    private static final long               serialVersionUID = -5694724140595312739L;
    private Queue<Request>                  jobQueue;
    private Queue<EagerResourceManager>     idleRM;
    private HeartbeatHandler                hHandler;
    private ScheduledExecutorService        timerScheduler;
    private Engine                          engine;
    private HashMap<String, Task<Object>>   status;
    private RmiServer                       rmiServer;
    private String                          myURL;
    private IRepository<IHeartbeatReceiver> repo;

    public GridScheduler(RmiServer rmiServer, IRepository<IHeartbeatReceiver> repo, String url) throws RemoteException, MalformedURLException {
        jobQueue = new LinkedBlockingQueue<>();
        idleRM = new CircularFifoQueue<>();
        myURL = url;
        status = new HashMap<String, Task<Object>>();
        this.repo = repo;
        this.rmiServer = rmiServer;
        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        hHandler = new HeartbeatHandler(repo);

    }

    public void checkConnections() {
        hHandler.getStatus();
    }

    public void addRM(EagerResourceManager rm) {
        idleRM.add(rm);
    }

    @Override
    public void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException {}

    /**
     * This method is called by the Simulation Launcher to kill the node. However it is also needed to kill the process, so we will have to implement
     * a mechanisim to kill all the nodes in a clean way, probably with Threads
     */
    @Override
    public void shutDown() {
        try {
            rmiServer.unRegister(myURL);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
