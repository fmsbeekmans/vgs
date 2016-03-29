package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.ResolvedValue;

import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class HeartbeatHandler2 extends UnicastRemoteObject implements IHeartbeatSender {

    private static final long        serialVersionUID = -9017016257581759403L;
    // private Map<Integer, String> destURL;
    private String                   destURL;
    private String                   originURL;
    private HashMap<Integer, Status> status;
    private IRepository<?>           repo;
    private Map<Integer, ?>          connections;
    private RmiServer                rmiServer;
    private Engine                   engine;
    private ScheduledExecutorService timerScheduler;
    private IHeartbeatSender         driver;
    private boolean                  receivedHeartbeat;
    private String                   s;
    // public HeartbeatHandler(String originURL, IRepository<?> repo, String type) {
    // this.repo = repo;
    // this.originURL = originURL;
    // status = new HashMap<Integer, Status>();
    // repo.ids().forEach(id -> {
    // status.put(id, Status.OFFLINE);
    // });
    // }

    public HeartbeatHandler2(String originURL, String destURL, RmiServer server) throws MalformedURLException, RemoteException, NotBoundException {
        this.destURL = destURL;
        this.originURL = originURL;
        this.rmiServer = server;
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        this.receivedHeartbeat = false;
        s = "OFF";
        register();
        while (!connect())
            ;
        sendHeartbeat();
        // checkLife();

    }

    public void register() throws MalformedURLException, RemoteException {
        rmiServer.register(originURL, this);
    }

    public boolean connect() throws MalformedURLException {
        if (driver == null) {
            try {
                driver = (IHeartbeatSender) Naming.lookup(destURL);
                return true;
            } catch (NotBoundException e) {
                return false;
            } catch (RemoteException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    public synchronized Promise<Boolean> isAlive() throws MalformedURLException, RemoteException, NotBoundException {
        ResolvedValue<Boolean> p = (ResolvedValue<Boolean>) Promises.value(receivedHeartbeat);
        return p;
    }

    private void checkLife() {
        final Task<Boolean> checkHeartbeat = Task.async(() -> isAlive()).withTimeout(5, TimeUnit.SECONDS);
        engine.run(checkHeartbeat);
        try {
            checkHeartbeat.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!checkHeartbeat.isFailed())
            s = "ON";
        System.out.println(!checkHeartbeat.isFailed() ? "Received result: " + checkHeartbeat.get() : "Error: " + checkHeartbeat.getError());
    }

    public void sendHeartbeat() throws MalformedURLException, RemoteException, NotBoundException {
        timerScheduler.scheduleAtFixedRate(() -> {
            Heartbeat h = new Heartbeat(originURL, destURL);
            try {
                driver.iAmAlive(h);
                s = "ON";
            } catch (Exception e) {
                if (e instanceof ConnectException) {
                    s = "OFF";
                }
            }
        } , 0, 5, TimeUnit.SECONDS);

    }

    public String getS() {
        return this.s;
    }

    @Override
    public synchronized void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException {
        //System.out.println("Received HB from: " + h.getSenderURL());
        receivedHeartbeat = true;
    }

}
