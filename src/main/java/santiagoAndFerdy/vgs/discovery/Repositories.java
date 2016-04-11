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
    public static IRepository<IUser> userRepository() throws IOException {
        InputStream userStream = Repositories.class.getClassLoader().getResourceAsStream("users");
        return Repository.fromStream(userStream);
    }

    public static IRepository<IResourceManager> resourceManagerRepository() throws IOException {
        InputStream rmStream = Repositories.class.getClassLoader().getResourceAsStream("resource-managers");
        return Repository.fromStream(rmStream);
    }

    public static IRepository<IGridScheduler> gridSchedulerRepository() throws IOException {
        InputStream gsStream = Repositories.class.getClassLoader().getResourceAsStream("grid-schedulers");
        return Repository.fromStream(gsStream);
    }
}