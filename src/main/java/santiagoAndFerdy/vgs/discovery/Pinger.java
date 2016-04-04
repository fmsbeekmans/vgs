package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.linkedin.parseq.EngineBuilder;

import santiagoAndFerdy.vgs.messages.Heartbeat;

public class Pinger<T extends IAddressable> {

    private IRepository<T>           repository;
    private int[]                      idsToIgnore;

    private ScheduledExecutorService timer;

    /**
     * Heartbeat handler which pings periodically the URLs in the repository.
     *
     * @param repository
     *            - repository containing the id and URLs
     * @param idsToIgnore
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public Pinger(IRepository<T> repository, int... idsToIgnore) {
        this.repository = repository;
        this.idsToIgnore = idsToIgnore;

        timer = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        checkLife();
    }

    public void stop() {
        timer.shutdownNow();
    }

    /**
     * This method schedules a task every 5 seconds which is looking for the IDs this handler should have connected and try to ping them. If it
     * success it will update the status to ONLINE otherwise (an exception either NotBoundException or ConnectionException) to OFFLINE.
     */
    public void checkLife() {
        timer.scheduleAtFixedRate(() -> {
            for (int id : repository.onlineIdsExcept(idsToIgnore)) {
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
