package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.cluster.EagerResourceManager;
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

public class GridScheduler extends UnicastRemoteObject {
    private static final long             serialVersionUID = -5694724140595312739L;
    private Queue<Request>                jobQueue;
    private Queue<EagerResourceManager>   idleRM;
    private HeartbeatHandler              hHandler;
    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;
    private HashMap<String, Task<Object>> status;
    private RmiServer rmiServer;
    
    public GridScheduler(RmiServer rmiServer) throws RemoteException, MalformedURLException {

        jobQueue = new LinkedBlockingQueue<>();
        idleRM = new CircularFifoQueue<>();
        status = new HashMap<String, Task<Object>>();
        this.rmiServer = rmiServer;
        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        
    }

    public void addRM(EagerResourceManager rm) {
        idleRM.add(rm);
    }
    public void startHBHandler() {
        try {
            hHandler = new HeartbeatHandler("localhost/hGS", "localhost/hRM", rmiServer);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}