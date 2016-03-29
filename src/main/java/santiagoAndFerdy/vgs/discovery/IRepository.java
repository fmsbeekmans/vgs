package santiagoAndFerdy.vgs.discovery;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 3/24/16.
 */
public interface IRepository<T extends Remote> extends Serializable {

    // for user
    Optional<T> getEntity(int id);
    Optional<Status> getLastKnownStatus(int id);

    /**
     * Updates the last known status of an RM
     * @param id the rm id
     * @param newStatus the state that the RM is currently thought to be in
     * @return true if the rm is known, otherwise false
     */
    boolean setLastKnownStatus(int id, Status newStatus);

    List<Integer> ids();
    Map<Integer, String> urls();
}
