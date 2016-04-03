package santiagoAndFerdy.vgs.user;

import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Fydio on 3/20/16.
 */
public class User extends UnicastRemoteObject implements IUser {
    private String url;

    private IRepository<IResourceManager> resourceManagerRepository;
    private Set<Job> pendingJobs;

    public User(IRepository<IResourceManager> resourceManagerRepository, String url) throws MalformedURLException, RemoteException {
        this.url = url;
        this.resourceManagerRepository = resourceManagerRepository;
        pendingJobs = new HashSet<>();
    }

    public void start(int rmId) throws RemoteException, NotBoundException, MalformedURLException {
        for(long i = 0; i < 1; i++) {
            final Job j = new Job(10000, i, rmId);
            IResourceManager resourceManager = resourceManagerRepository.getEntity(rmId).get();
            pendingJobs.add(j);
            resourceManager.offerWork(new WorkRequest(url, j));
        }
    }

    @Override
    public void acceptResult(Job j) throws RemoteException {
        pendingJobs.remove(j);
    }
}
