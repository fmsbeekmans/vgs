package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;

import santiagoAndFerdy.vgs.messages.Heartbeat;

public class HeartbeatHandler {

    private HashMap<Integer, Status> status;
    private IRepository<?> repository;
    private Engine                   engine; //dunno if we need the Engine here actually
    private ScheduledExecutorService timerScheduler;

    public HeartbeatHandler(IRepository<? extends IAdressable> repository) throws MalformedURLException, RemoteException {
        status = new HashMap<>();
        this.repository = repository;
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        checkLife();
    }
    /**
     * Print the status of the IDs connected to this handler.
     */
    public void getStatus() {
        for (int id : status.keySet()) {
            System.out.println(repository.getUrl(id) + " is " + status.get(id));
        }
    }

    /**
     * This method schedules a task every 5 seconds which is looking for the IDs this handler should have connected and try to ping them. If it
     * success it will update the status to ONLINE otherwise (an exception either NotBoundException or ConnectionException) to OFFLINE.
     */
    public void checkLife() {
        timerScheduler.scheduleAtFixedRate(() -> {
            for (int id : repository.ids()) {
                Heartbeat h = new Heartbeat(repository.getUrl(id));
                try {
                    IAdressable driver;
                    driver = (IAdressable) Naming.lookup(repository.getUrl(id));
                    driver.iAmAlive(h);
                    status.put(id, Status.ONLINE);
                } catch (Exception e) {
//                    if(urls.get(id).contains("52.58.103.62")) testing for amazon (doesn't work yet)
//                        e.printStackTrace();
                    status.put(id, Status.OFFLINE);
                }
            }
        } , 0, 5, TimeUnit.SECONDS);

    }
}
