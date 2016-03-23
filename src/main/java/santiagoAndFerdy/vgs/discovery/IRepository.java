package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerUserClient;

import java.io.Serializable;
import java.util.Optional;

/**
 * Created by Fydio on 3/24/16.
 */
public interface IRepository extends Serializable {

    // for user
    Optional<IResourceManagerDriver> getResourceManager(int id);
    Optional<Status> getLastKnownResourceManagerStatus(int id);

    /**
     * Updates the last known status of an RM
     * @param id the rm id
     * @param newStatus the state that the RM is currently thought to be in
     * @return true if the rm is known, otherwise false
     */
    boolean setLastKnownResourceManagerStatus(int id, Status newStatus);

    // for resource managers
    Optional<IResourceManagerUserClient> getUserClient(int id);
    Optional<Status> getLastKnownUserClientStatus(int id);
    /**
     * Updates the last known status of an RM
     * @param id the rm id
     * @param newStatus the state that the RM is currently thought to be in
     * @return true if the rm is known, otherwise false
     */
    boolean setLastKnownUserStatus(int id, Status newStatus);
}
