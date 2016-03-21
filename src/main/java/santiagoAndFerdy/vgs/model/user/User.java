package santiagoAndFerdy.vgs.model.user;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerProxy;
import santiagoAndFerdy.vgs.model.cluster.ResourceManagerProxy;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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

    private IResourceManagerProxy rm;

    public User(RmiServer rmiServer, String resourceManagerProxyUrl, String resourceManagerUrl) throws MalformedURLException, RemoteException {
        final int numCores = Runtime.getRuntime().availableProcessors();
        taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        timerScheduler = Executors.newSingleThreadScheduledExecutor();

        engine = new EngineBuilder()
                .setTaskExecutor(taskScheduler)
                .setTimerScheduler(timerScheduler)
                .build();

        rm = new ResourceManagerProxy(this, rmiServer, resourceManagerProxyUrl, resourceManagerUrl);
    }

    public void start() throws MalformedURLException, RemoteException, NotBoundException {

        for(long i = 0; i < 1; i++) {
            final Job j = new Job(1000, i, 0);
            Task<Void> execution = rm.schedule(j);
            execution.andThen(Task.action(() -> System.out.println("Finished task " + j.getJobId())));

            engine.run(execution);
        }
    }
}
