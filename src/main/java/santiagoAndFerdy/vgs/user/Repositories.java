package santiagoAndFerdy.vgs.user;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;

import java.io.IOException;
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
            URL usersUrl = classLoader.getResource("users");
            Path userRepositoryFilePath = Paths.get(usersUrl.toURI());

            USER_REPOSITORY = Repository.fromFile(userRepositoryFilePath);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            URL resourceManagerUrl = classLoader.getResource("resource-managers");
            Path resourceManagerRepositoryFilePath = Paths.get(resourceManagerUrl.toURI());

            RESOURCE_MANAGER_REPOSITORY = Repository.fromFile(resourceManagerRepositoryFilePath);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            URL gridSchedulersUrl = classLoader.getResource("grid-schedulers");
            Path gridSchedulersFilePath = Paths.get(gridSchedulersUrl.toURI());

            GRID_SCHEDULER_REPOSITORY = Repository.fromFile(gridSchedulersFilePath);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
