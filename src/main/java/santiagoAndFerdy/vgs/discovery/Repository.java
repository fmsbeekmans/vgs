package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerUserClient;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 3/24/16.
 */
public class Repository implements IRepository {

    private String[] userUrls;
    private Status[] userStatuses;
    private String[] resourceManagerUrls;
    private Status[] resourceManagerStatuses;

    public Repository(Map<Integer, String> userUrls, Map<Integer, String> resourceManagerUrls) {
        int nUsers = userUrls.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        this.userUrls = new String[nUsers];
        this.userStatuses = new Status[nUsers];

        for(int k : userUrls.keySet()) {
            this.userUrls[k] = userUrls.get(k);
            this.userStatuses[k] = Status.OFFLINE;
        }

        int nResourceManagers = userUrls.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        this.resourceManagerUrls = new String[nResourceManagers];
        this.resourceManagerStatuses = new Status[nResourceManagers];

        for(int k : resourceManagerUrls.keySet()) {
            this.resourceManagerUrls[k] = resourceManagerUrls.get(k);
            this.resourceManagerStatuses[k] = Status.OFFLINE;
        }
    }

    @Override
    public Optional<IResourceManagerDriver> getResourceManager(int id) {
        return Optional.ofNullable(resourceManagerUrls[id])
                .flatMap(url -> {
                    try {
                        return Optional.of((IResourceManagerDriver) Naming.lookup(url));
                    } catch (NotBoundException | MalformedURLException | RemoteException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Optional<Status> getLastKnownResourceManagerStatus(int id) {
        return Optional.ofNullable(resourceManagerStatuses[id]);
    }

    @Override
    public boolean setLastKnownResourceManagerStatus(int id, Status newStatus) {
        if(resourceManagerStatuses[id] != null) {
            resourceManagerStatuses[id] = newStatus;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Optional<IResourceManagerUserClient> getUserClient(int id) {
        return Optional.ofNullable(userUrls[id])
                .flatMap(url -> {
                    try {
                        return Optional.of((IResourceManagerUserClient) Naming.lookup(url));
                    } catch (NotBoundException | MalformedURLException | RemoteException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Optional<Status> getLastKnownUserClientStatus(int id) {
        return Optional.ofNullable(userStatuses[id]);
    }

    @Override
    public boolean setLastKnownUserStatus(int id, Status newStatus) {
        if(userStatuses[id] != null) {
            userStatuses[id] = newStatus;
            return true;
        } else {
            return false;
        }
    }
}
