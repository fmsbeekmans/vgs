package santiagoAndFerdy.vgs.model.cluster;

import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.model.user.User;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 3/20/16.
 */
public class ResourceManagerProxy extends UnicastRemoteObject implements IResourceManagerProxy {
    private User user;
    private RmiServer rmiServer;
    private String url;
    private String driverUrl;
    private IResourceManagerDriver driver;

    private Map<Job, SettablePromise<Void>> pendingJobs;

    public ResourceManagerProxy(@NotNull User user, @NotNull RmiServer rmiServer, @NotNull String url, @NotNull String driverUrl) throws RemoteException, MalformedURLException {
        this.rmiServer = rmiServer;
        this.url = url;
        this.driverUrl = driverUrl;
        this.user = user;
        register();

        pendingJobs = new HashMap<>();
        connect();
    }

    public void register() throws MalformedURLException, RemoteException {
        rmiServer.register(url, this);
    }

    public boolean connect() throws MalformedURLException {
        if(driver == null) {
            try {
                driver = (IResourceManagerDriver) Naming.lookup(driverUrl);

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

    @Override
    public void acceptResult(@NotNull Job j) {
        SettablePromise<Void> promise = pendingJobs.get(j);

        if(promise != null) {
            promise.done(null);

            System.out.println("Done");
        }
    }

    @Override
    public synchronized Task<Void> schedule(@NotNull Job j) throws MalformedURLException, RemoteException, NotBoundException {
        connect();
        SettablePromise<Void> completionPromise = Promises.settable();
        pendingJobs.put(j, completionPromise);
        driver.queue(new Request(j, this));

        return Task.async(() -> completionPromise);
    }

    @Override
    public String getUrl() {
        return url;
    }
}
