package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerUserClient;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Created by Fydio on 3/24/16.
 */
public class Repository<T extends Serializable> implements IRepository<T> {

    private String[] urls;
    private Status[] statuses;

    public Repository(Map<Integer, String> urls) {
        int n = urls.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        this.urls = new String[n];
        this.statuses = new Status[n];

        for(int k : urls.keySet()) {
            this.urls[k] = urls.get(k);
            this.statuses[k] = Status.OFFLINE;
        }
    }

    @Override
    public Optional<T> getEntity(int id) {
        return Optional.ofNullable(urls[id])
                .flatMap(url -> {
                    try {
                        return Optional.of((T) Naming.lookup(url));
                    } catch (NotBoundException | MalformedURLException | RemoteException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Optional<Status> getLastKnownStatus(int id) {
        return Optional.ofNullable(statuses[id]);
    }

    @Override
    public boolean setLastKnownStatus(int id, Status newStatus) {
        if(statuses[id] != null) {
            statuses[id] = newStatus;
            return true;
        } else {
            return false;
        }
    }

    public static <T extends Serializable> IRepository<T> fromFile(Path entityListingPath) throws IOException {
        Scanner s = new Scanner(Files.newInputStream(entityListingPath));

        Map<Integer, String> urls = new HashMap<>();

        while(s.hasNext()) {
            int id = s.nextInt();
            String url = s.next();

            urls.put(id, url);
        }

        return new Repository<T>(urls);
    }
}
