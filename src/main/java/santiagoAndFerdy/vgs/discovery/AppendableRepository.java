package santiagoAndFerdy.vgs.discovery;

import java.rmi.Remote;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by Fydio on 3/24/16.
 */
public class AppendableRepository<T extends Remote> extends Repository<T> {
    public AppendableRepository(Map urls) {
        super(urls);
    }

    public void put(Integer id, String url) {
        if (id >= urls.length) {
            urls = Arrays.copyOf(urls, id + 1);
            statuses = Arrays.copyOf(statuses, id + 1);
        }

        urls[id] = url;
        statuses[id] = Status.OFFLINE;
    }
}
