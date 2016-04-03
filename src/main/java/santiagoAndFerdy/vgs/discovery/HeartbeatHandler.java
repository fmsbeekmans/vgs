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

    private IRepository<?>           repository;
    private Engine                   engine;        // dunno if we need the Engine here actually
    private ScheduledExecutorService timerScheduler;
    private int                      id;
    private boolean                  flag;

    /**
     * Heartbeat handler which pings periodically the URLs in the repository.
     * 
     * @param repository
     *            - repository containing the id and URLs
     * @param id
     *            - id of the component which creates the handler, it's used to avoid pinging itself
     * @param flag
     *            - flag indicating whether should check not pinging the same id than the received in the id param or not. The GS needs to ping all
     *            RMs but not all GS (not itself)
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public HeartbeatHandler(IRepository<? extends IAddressable> repository, int id, boolean flag) throws MalformedURLException, RemoteException {
        this.repository = repository;
        this.id = id;
        this.flag = flag;
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        checkLife();
    }

    /**
     * This method schedules a task every 5 seconds which is looking for the IDs this handler should have connected and try to ping them. If it
     * success it will update the status to ONLINE otherwise (an exception either NotBoundException or ConnectionException) to OFFLINE.
     */
    public void checkLife() {
        timerScheduler.scheduleAtFixedRate(() -> {
            for (int id : repository.onlineIdsExcept()) {
                if (id == this.id && flag) // to not ping himself
                    continue;
                Heartbeat h = new Heartbeat(repository.getUrl(id));
                try {
                    IAddressable driver;
                    driver = (IAddressable) Naming.lookup(repository.getUrl(id));
                    driver.iAmAlive(h);
                    repository.setLastKnownStatus(id, Status.ONLINE);
                } catch (Exception e) {
                    // if(urls.get(id).contains("52.58.103.62")) testing for amazon (doesn't work yet)
                    // e.printStackTrace();
                    repository.setLastKnownStatus(id, Status.OFFLINE);
                }
            }
        } , 0, 5, TimeUnit.SECONDS);

    }
}
