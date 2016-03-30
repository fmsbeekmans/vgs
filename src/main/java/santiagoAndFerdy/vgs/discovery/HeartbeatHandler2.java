package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private static final long              serialVersionUID = -9017016257581759403L;

    private String                         originURL;
    private HashMap<Integer, Status>       status;
    private IRepository<IHeartbeatSender>  repo;
    private Map<Integer, IHeartbeatSender> connections;
    private Map<Integer, String>           urls;
    private RmiServer                      rmiServer;
    private Engine                         engine;
    private ScheduledExecutorService       timerScheduler;

    public HeartbeatHandler2(String originURL, IRepository<IHeartbeatSender> repo, RmiServer server) throws MalformedURLException, RemoteException {
        this.repo = repo;
        this.originURL = originURL;
        rmiServer = server;
        status = new HashMap<Integer, Status>();
        connections = new HashMap<Integer, IHeartbeatSender>();
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        register();
    }
    
    public void connectHandler(){
        try {
            while (!connect());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sendHeartbeat();
    }
    
    public void register() throws MalformedURLException, RemoteException {
        rmiServer.register(originURL, this);
    }

    public boolean connect() throws MalformedURLException {
        try {
            urls = repo.urls();
            for (int id : urls.keySet()) {
                IHeartbeatSender driver;
                if (!connections.containsKey(id)) {
                    driver = (IHeartbeatSender) Naming.lookup(urls.get(id));
                    connections.put(id, driver);
                    System.out.println(originURL + " connected to: " + urls.get(id));
                }
            }
            return true;
        } catch (NotBoundException e) {
            // e.printStackTrace();
            return false;
        } catch (RemoteException e) {
            return false;
        }

    }

    public void checkLife() {
        for (int id : status.keySet()) {
            System.out.println(id + " is " + status.get(id));
        }
    }

    public boolean allConnected() {
        return connections.size() == urls.size();
    }

    public void sendHeartbeat() {
        timerScheduler.scheduleAtFixedRate(() -> {
            for (int id : connections.keySet()) {
                Heartbeat h = new Heartbeat(originURL, urls.get(id));
                //System.out.println("Sending to: " + urls.get(id));
                try {
                    connections.get(id).iAmAlive(h);
                    status.put(id, Status.ONLINE);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                        status.put(id, Status.OFFLINE);

                }
            }
        } , 0, 5, TimeUnit.SECONDS);

    }

    @Override
    public synchronized void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException {
        //System.out.println("I am alive and I am: " + originURL);
    }

}
