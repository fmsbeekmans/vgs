package santiagoAndFerdy.vgs.discovery;

import com.linkedin.parseq.function.Function2;
import santiagoAndFerdy.vgs.discovery.selector.ISelector;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Fydio on 3/24/16.
 */
public class Repository<T extends Remote> implements IRepository<T> {
    private static final long             serialVersionUID = 1619009373620002568L;

    protected Map<Integer, String> urls;
    protected Map<Integer, Status> statuses;
    protected Map<Integer, Long> loads;


    private List<Function<Integer, Void>> offlineCallbacks;
    private List<Function<Integer, Void>> onlineCallbacks;

    public Repository(Map<Integer, String> urls) {
        int n = urls.keySet().stream().max(Comparator.naturalOrder()).map(max -> max + 1).orElse(0);
        this.urls = urls;
        this.statuses = new HashMap<>();
        this.loads = new HashMap<>();
        for(int id : this.urls.keySet()) {
            statuses.put(id, Status.OFFLINE);
            loads.put(id, Long.MAX_VALUE);
        }

        offlineCallbacks = new LinkedList<>();
        onlineCallbacks = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<T> getEntity(int id) {
        try {
            T result = (T) Naming.lookup(urls.get(id));
            setLastKnownStatus(id, Status.ONLINE);

            return Optional.of(result);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            setLastKnownStatus(id, Status.OFFLINE);

            return Optional.empty();
        }
    }

    @Override
    public Status getLastKnownStatus(int id) {
        return statuses.get(id);
    }

    @Override
    public boolean setLastKnownStatus(int id, Status newStatus) {
        if (statuses.get(id) != null) {
            Status oldStatus = getLastKnownStatus(id);
            if (oldStatus != newStatus) {

                if (newStatus == Status.ONLINE)
                    executeOnlineCallbacks(id);
                if (newStatus == Status.OFFLINE) {
                    loads.remove(id);
                    executeOfflineCallbacks(id);
                }

                statuses.put(id, newStatus);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public Map<Integer, Long> getLastKnownLoads() {
        Map<Integer, Long> clone = new HashMap<>();

        for (int id : loads.keySet()) clone.put(id, loads.get(id));

        return clone;
    }

    @Override
    public long getLastKnownLoad(int id) {
        return loads.get(id);
    }

    @Override
    public void setLastKnownLoad(int id, long load) {
        loads.put(id, load);
    }

    @Override
    public List<Integer> ids() {
        return urls.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public List<Integer> idsExcept(int... except) {
        Set<Integer> exceptions = new HashSet<>();
        for (int exception : except)
            exceptions.add(exception);

        return ids().stream().filter(i -> !exceptions.contains(i)).collect(Collectors.toList());
    }

    @Override
    public List<Integer> onlineIdsExcept(int... except) {
        Set<Integer> exceptions = new HashSet<>();
        for (int exception : except)
            exceptions.add(exception);

        return ids().stream().filter(i -> !exceptions.contains(i) && getLastKnownStatus(i) == Status.ONLINE).collect(Collectors.toList());
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
        for (Function<Integer, Void> offlineCallback : offlineCallbacks) {
            offlineCallback.apply(onlineId);
        }
    }

    @Override
    public String getUrl(int id) {
        return urls.get(id);
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

    public static <T extends Remote> IRepository<T> fromStream(InputStream input) throws IOException {
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

    @Override
    public Optional<T> getEntityExceptId(int id) {
        setLastKnownStatus(id, Status.OFFLINE);
        try {
            List<String> l = urls.values().stream().collect(Collectors.toList());
            l.remove(urls.get(id));
            int i = ThreadLocalRandom.current().nextInt(0, l.size() - 1);
            T result = (T) Naming.lookup(urls.get(i));
            return Optional.of(result);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            return Optional.empty();
        }
    }

    @Override
    public <R> Optional<R> invokeOnEntity(Function2<T, Integer, R> toInvoke, ISelector selector, int... idsToIgnore) {
        Map<Integer, Long> weights = getLastKnownLoads();
        Arrays.stream(idsToIgnore).forEach(weights::remove);
        final boolean[] success = new boolean[1];
        success[0] = false;

        while(!success[0]) {
            Optional<Integer> selectedId = selector.selectIndex(weights);

            Optional<R> attempt = selectedId.flatMap(id -> {
                weights.remove(selectedId);

                return getEntity(id).map(entity -> {
                    R result = null;
                    try {
                        result = toInvoke.apply(entity, id);
                        success[0] = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return result;
                });
            });

            if(attempt.isPresent()) return attempt;
        }

        return Optional.empty();
    }
}
