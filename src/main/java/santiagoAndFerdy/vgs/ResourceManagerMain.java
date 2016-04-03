package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
        int id = 0;
//        int id = Integer.valueOf(args[1]);

        RmiServer rmiServer = new RmiServer(1099);
        new ResourceManager(
                rmiServer,
                id,
                Repositories.USER_REPOSITORY,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                Repositories.GRID_SCHEDULER_REPOSITORY,
                4);
    }
}
