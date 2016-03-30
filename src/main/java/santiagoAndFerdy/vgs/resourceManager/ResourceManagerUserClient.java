package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.user.User;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Fydio on 3/20/16.
 */
public class ResourceManagerUserClient extends UnicastRemoteObject implements IResourceManagerUserClient {
    private User user;
    private RmiServer rmiServer;
    private String url;
    private int id;
    private IRepository<IResourceManagerDriver> driverRepository;
    private IResourceManagerDriver driver;

    private Engine engine;

    private Map<Job, SettablePromise<Void>> pendingJobs;

    public ResourceManagerUserClient(@NotNull User user, @NotNull RmiServer rmiServer, @NotNull String url, int id, IRepository<IResourceManagerDriver> driverRepository) throws RemoteException, MalformedURLException {
        this.rmiServer = rmiServer;
        this.url = url;
        this.id = id;
        this.driverRepository = driverRepository;
        this.user = user;
        register();

        pendingJobs = new HashMap<>();
        connect();

        // setup parseq engine
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        ScheduledExecutorService timerScheduler = Executors.newSingleThreadScheduledExecutor();

        engine = new EngineBuilder()
                .setTaskExecutor(taskScheduler)
                .setTimerScheduler(timerScheduler)
                .build();

    }

    public void register() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);
    }

    public boolean connect() throws MalformedURLException {
        if(driver == null) {
            Optional<IResourceManagerDriver> maybeDriver = driverRepository.getEntity(id);
            if (maybeDriver.isPresent()) {
                driver = maybeDriver.get();
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    public void acceptResult(@NotNull Job j) {
        SettablePromise<Void> promise = pendingJobs.get(j);

        if(promise != null) {
            promise.done(null);
        }
    }

    @Override
    public synchronized Promise<Void> schedule(@NotNull Job j) throws MalformedURLException, RemoteException, NotBoundException {
        connect();
        System.out.println("Scheduling job " + j.getJobId());
        SettablePromise<Void> completionPromise = Promises.settable();
        pendingJobs.put(j, completionPromise);

        Task<Void> queue = Task.action(() -> driver.queue(new UserRequest(j, this)));
        engine.run(queue);

        return completionPromise;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
