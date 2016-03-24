package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerUserClient;

import java.io.Serializable;
import java.util.Optional;

/**
 * Created by Fydio on 3/24/16.
 */
public interface IRepository<T extends Serializable> extends Serializable {

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
}
