package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {

    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {
        //int id = 1;
         int id = Integer.parseInt(args[0]);

        RmiServer server = new RmiServer(1099);

        new GridScheduler(server, id, IRepository.Repositories.resourceManagerRepository, IRepository.Repositories.gridSchedulerRepository);
    }
}
