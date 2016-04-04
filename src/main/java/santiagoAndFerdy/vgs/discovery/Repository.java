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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Fydio on 3/24/16.
 */
public class Repository<T extends Remote> implements IRepository<T> {
    private static final long serialVersionUID = 1619009373620002568L;

    protected String[]        urls;
    protected Status[]        statuses;

    private List<Function<Integer, Void>> offlineCallbacks;
    private List<Function<Integer, Void>> onlineCallbacks;

    public Repository(Map<Integer, String> urls) {
        int n = urls.keySet().stream().max(Comparator.naturalOrder()).map(max -> max + 1).orElse(0);
        this.urls = new String[n];
        this.statuses = new Status[n];

        for (int k : urls.keySet()) {
            this.urls[k] = urls.get(k);
            this.statuses[k] = Status.OFFLINE;
        }

        offlineCallbacks = new LinkedList<>();
        onlineCallbacks = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> getEntity(int id) {
        try {
            T result = (T) Naming.lookup(urls[id]);
            setLastKnownStatus(id, Status.ONLINE);

            return Optional.of(result);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            setLastKnownStatus(id, Status.OFFLINE);

            return Optional.empty();
        }
    }

    @Override
    public Status getLastKnownStatus(int id) {
        return statuses[id];
    }

    @Override
    public boolean setLastKnownStatus(int id, Status newStatus) {
        if (statuses[id] != null) {
            Status oldStatus = getLastKnownStatus(id);
            if(oldStatus != newStatus) {

                if(newStatus == Status.ONLINE) executeOnlineCallbacks(id);
                if(newStatus == Status.OFFLINE) executeOfflineCallbacks(id);

                statuses[id] = newStatus;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Integer> ids() {
        if (urls.length == 0)
            return new LinkedList<>();

        return IntStream.range(0, urls.length).filter(i -> urls[i] != null).mapToObj(i -> new Integer(i)).collect(Collectors.toList());
    }

    @Override
    public List<Integer> idsExcept(int... except) {
        Set<Integer> exceptions = new HashSet<>();
        for (int exception : except) exceptions.add(exception);

        return ids().stream()
                .filter(i -> !exceptions.contains(i))
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> onlineIdsExcept(int... except) {
        Set<Integer> exceptions = new HashSet<>();
        for (int exception : except) exceptions.add(exception);

        return ids().stream()
                .filter(i -> !exceptions.contains(i) && getLastKnownStatus(i) == Status.ONLINE)
                .collect(Collectors.toList());
    }

    @Override
    public void onOffline(Function<Integer, Void> doWithOfflineId) {
        offlineCallbacks.add(doWithOfflineId);
    }

    @Override
    public void onOnline(Function<Integer, Void> doWithOnlineId) {
        onlineCallbacks.add(doWithOnlineId);
    }

    public void executeOnlineCallbacks(int onlineId) {
        for (Function<Integer, Void> onlineCallback : onlineCallbacks) {
            onlineCallback.apply(onlineId);
        }
    }

    public void executeOfflineCallbacks(int onlineId) {
        for (Function<Integer, Void> offlineCallback : onlineCallbacks) {
            offlineCallback.apply(onlineId);
        }
    }

    @Override
    public String getUrl(int id) {
        return urls[id];
    }

    public static <T extends Remote> IRepository<T> fromFile(Path entityListingPath) throws IOException {
        Scanner s = new Scanner(Files.newInputStream(entityListingPath));

        Map<Integer, String> urls = new HashMap<>();

        while (s.hasNext()) {
            int id = s.nextInt();
            String url = s.next();

            urls.put(id, url);
        }

        return new Repository<T>(urls);
    }

    public static <T extends Remote> IRepository<T> fromS3(InputStream input) throws IOException {
        Scanner s = new Scanner(input);
        Map<Integer, String> urls = new HashMap<>();
        while (s.hasNext()) {
            int id = s.nextInt();
            String url = s.next();
            urls.put(id, url);
        }
        s.close();
        return new Repository<T>(urls);
    }
}
