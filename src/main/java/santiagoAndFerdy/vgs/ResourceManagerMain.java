package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
        if (args.length < 3) {
            System.err.println("Please insert range of RM IDs and the number of nodes");
            return;
        }
        int begin = Integer.valueOf(args[0]);
        int end = Integer.valueOf(args[1]);
        int nNodes = Integer.valueOf(args[2]);

        RmiServer rmiServer = new RmiServer(1099);

        Repositories.resourceManagerRepository().ids().forEach(rmId -> {
            if (rmId >= begin && rmId <= end) {
                try {
                    new ResourceManager(rmiServer, rmId, Repositories.userRepository(), Repositories.resourceManagerRepository(),
                            Repositories.gridSchedulerRepository(), nNodes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}