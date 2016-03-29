package santiagoAndFerdy.vgs.user;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerDriver;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerUserClient;
import santiagoAndFerdy.vgs.resourceManager.ResourceManagerUserClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Fydio on 3/20/16.
 */
public class User {
    private ExecutorService taskScheduler;
    private ScheduledExecutorService timerScheduler;
    private Engine engine;
    private IRepository<IResourceManagerDriver> rmRepository;
    private Map<Integer, IResourceManagerUserClient> rms;

    public User(RmiServer rmiServer, String resourceManagerProxyUrl, IRepository<IResourceManagerDriver> rmRepository) throws MalformedURLException, RemoteException {
        final int numCores = Runtime.getRuntime().availableProcessors();
        taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        this.rmRepository = rmRepository;

        engine = new EngineBuilder()
                .setTaskExecutor(taskScheduler)
                .setTimerScheduler(timerScheduler)
                .build();

        rms = new HashMap<>();
        rmRepository.ids().forEach(rmId -> {
            try {
                rms.put(rmId, new ResourceManagerUserClient(this, rmiServer, resourceManagerProxyUrl, rmId, rmRepository));
                start(rmId);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    public Promise<Void> start(int rmId) {
        for(long i = 0; i < 2; i++) {
            final Job j = new Job(10000, i, rmId);
            IResourceManagerUserClient rm = rms.get(rmId);

            try {
                Promise<Void> execution = rm.schedule(j);
                execution.addListener(e -> System.out.println("Finished task " + j.getJobId()));
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return Promises.settable();
    }
}
