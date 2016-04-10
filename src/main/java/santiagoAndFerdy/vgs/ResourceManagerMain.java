package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
        if(args.length < 1){
            System.err.println("Please insert this RM ID");
            return;
        }
        
        int id = Integer.valueOf(args[0]);
        // int nNodes = Integer.valueOf(args[1]);
        int nNodes = 1000;
        RmiServer rmiServer = new RmiServer(1099);
        new ResourceManager(rmiServer, id, Repositories.userRepository, Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository, nNodes);
    }
}
