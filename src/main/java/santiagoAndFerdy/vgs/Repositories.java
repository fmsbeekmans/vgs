package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.user.IUser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Fydio on 4/3/16.
 */
public class Repositories {
    public static IRepository<IUser> USER_REPOSITORY;
    public static IRepository<IResourceManager> RESOURCE_MANAGER_REPOSITORY;
    public static IRepository<IGridScheduler> GRID_SCHEDULER_REPOSITORY;

    static {
        ClassLoader classLoader = Repositories.class.getClassLoader();

        try {
            InputStream usersStream = classLoader.getResourceAsStream("users");
            //Path userRepositoryFilePath = Paths.get(usersUrl.);

            USER_REPOSITORY = Repository.fromStream(usersStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream resourceManagerStream = classLoader.getResourceAsStream("resource-managers");
            //Path resourceManagerRepositoryFilePath = Paths.get(resourceManagerUrl.toURI());

            RESOURCE_MANAGER_REPOSITORY = Repository.fromStream(resourceManagerStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream gridSchedulersStream = classLoader.getResourceAsStream("grid-schedulers");
            //Path gridSchedulersFilePath = Paths.get(gridSchedulersUrl.toURI());

            GRID_SCHEDULER_REPOSITORY = Repository.fromStream(gridSchedulersStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
