package santiagoAndFerdy.vgs.discovery;

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

    /**
     * Created by Fydio on 4/3/16.
     */
    class Repositories {
        public static IRepository<IUser> userRepository;
        public static IRepository<IResourceManager> resourceManagerRepository;
        public static IRepository<IGridScheduler> gridSchedulerRepository;

        static {
            ClassLoader classLoader = Repositories.class.getClassLoader();

            try {
                InputStream usersStream = classLoader.getResourceAsStream("users");
                //Path userRepositoryFilePath = Paths.get(usersUrl.);

                userRepository = Repository.fromStream(usersStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                InputStream resourceManagerStream = classLoader.getResourceAsStream("resource-managers");
                //Path resourceManagerRepositoryFilePath = Paths.get(resourceManagerUrl.toURI());

                resourceManagerRepository = Repository.fromStream(resourceManagerStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                InputStream gridSchedulersStream = classLoader.getResourceAsStream("grid-schedulers");
                //Path gridSchedulersFilePath = Paths.get(gridSchedulersUrl.toURI());

                gridSchedulerRepository = Repository.fromStream(gridSchedulersStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
