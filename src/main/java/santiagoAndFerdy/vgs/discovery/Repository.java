package santiagoAndFerdy.vgs.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Fydio on 3/24/16.
 */
public class Repository<T extends Remote> implements IRepository<T> {
    protected String[] urls;
    protected Status[] statuses;

    public Repository(Map<Integer, String> urls) {
        int n = urls.keySet().stream()
                .max(Comparator.naturalOrder())
                .map(max -> max + 1).orElse(0);
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

    @Override
    public List<Integer> ids() {
        if(urls.length == 0) return new LinkedList<>();

        return IntStream
                .range(0, urls.length)
                .filter(i -> urls[i] != null)
                .mapToObj(i -> new Integer(i))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, String> urls() {
        Map<Integer, String> urlMap = new HashMap<Integer, String>();

        for (int i = 0; i < urls.length; i++) {
            if(urls[i] != null) urlMap.put(i, urls[i]);
        }

        return urlMap;
    }

    public static <T extends Remote> IRepository<T> fromFile(Path entityListingPath) throws IOException {
        Scanner s = new Scanner(Files.newInputStream(entityListingPath));

        Map<Integer, String> urls = new HashMap<>();

        while(s.hasNext()) {
            int id = s.nextInt();
            String url = s.next();

            urls.put(id, url);
        }

        return new Repository<T>(urls);
    }
    
    public static <T extends Remote> IRepository<T> fromS3(InputStream input) throws IOException {
        Scanner s = new Scanner(input);
        Map<Integer, String> urls = new HashMap<>();
        while(s.hasNext()) {
            int id = s.nextInt();
            String url = s.next();
            urls.put(id, url);
        }
        return new Repository<T>(urls);
    }
}
