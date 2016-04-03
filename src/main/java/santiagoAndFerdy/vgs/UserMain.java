package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.Repositories;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {

        int id = 0;
//        int id = Integer.parseInt(args[0]);

        RmiServer rmiServer = new RmiServer(1099);

        new User(
                rmiServer,
                Repositories.USER_REPOSITORY,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                id).createJobs(0, 10);


    }
}
