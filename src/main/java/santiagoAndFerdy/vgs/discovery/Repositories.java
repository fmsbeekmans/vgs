package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.user.IUser;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Fydio on 4/3/16.
 */
public class Repositories {
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
