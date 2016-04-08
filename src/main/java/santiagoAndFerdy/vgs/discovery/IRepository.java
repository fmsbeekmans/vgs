package santiagoAndFerdy.vgs.discovery;

import com.linkedin.parseq.function.Function2;
import santiagoAndFerdy.vgs.discovery.selector.ISelector;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.user.IUser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by Fydio on 3/24/16.
 */
public interface IRepository<T extends Remote> extends Serializable {

    // for user
    Optional<T> getEntity(int id);
    String getUrl(int id);
    Status getLastKnownStatus(int id);
    /**
     * Updates the last known status of an RM
     * @param id the rm id
     * @param newStatus the state that the RM is currently thought to be in
     * @return true if the rm is known, otherwise false
     */
    boolean setLastKnownStatus(int id, Status newStatus);
    long getLastKnownLoad(int id);

    void setLastKnownLoad(int id, long load);

    Map<Integer, Long> getLastKnownLoads();

    List<Integer> ids();

    List<Integer> onlineIdsExcept(int... except);

    List<Integer> idsExcept(int... except);

    void onOffline(Function<Integer, Void> doWithOfflineId);

    void onOnline(Function<Integer, Void> doWithOnlineId);
    
    Optional<T> getEntityExceptId(int id);

    <R> Optional<R> invokeOnEntity(Function2<T, Integer, R> toInvoke, ISelector selector, int... idsToIgnore);

}
