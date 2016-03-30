package santiagoAndFerdy.vgs.gridScheduler;

import java.net.MalformedURLException;
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

import santiagoAndFerdy.vgs.discovery.HeartbeatHandler2;
import santiagoAndFerdy.vgs.discovery.IHeartbeatSender;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class GridScheduler extends UnicastRemoteObject implements Runnable {
    private static final long             serialVersionUID = -5694724140595312739L;
    private Queue<Request>                jobQueue;
    private Queue<EagerResourceManager>   idleRM;
    private HeartbeatHandler2             hHandler;
    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;
    private HashMap<String, Task<Object>> status;
    private String                        myURLForHB;
    private String                        myURL;
    private IRepository<IHeartbeatSender> repoForHB;
    private RmiServer                     rmiServer;
    private boolean                       running;
    private Thread                        pollingThread;

    public GridScheduler(RmiServer rmiServer, IRepository<IHeartbeatSender> repo, String url) throws RemoteException, MalformedURLException {
        jobQueue = new LinkedBlockingQueue<>();
        idleRM = new CircularFifoQueue<>();
        myURLForHB = url + "-hb";
        myURL = url;
        status = new HashMap<String, Task<Object>>();
        this.rmiServer = rmiServer;
        repoForHB = repo;

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        hHandler = new HeartbeatHandler2(myURLForHB, repoForHB, rmiServer);
        pollingThread = new Thread(this);
        running = true;
        pollingThread.start();
    }

    public void checkConnections() {
        hHandler.checkLife();
    }

    public void addRM(EagerResourceManager rm) {
        idleRM.add(rm);
    }

    public void startHBHandler() {
        if(hHandler != null){
            hHandler.connectHandler();
        }
    }

    @Override
    public void run() {
        while (running) {
//logic
        }

    }

    public void shutdown() {
        try {
            rmiServer.unRegister(myURLForHB);
            rmiServer.unRegister(myURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        running = false;
        try {
            pollingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}