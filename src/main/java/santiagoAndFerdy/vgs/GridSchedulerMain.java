package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {

    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException{
        if (args.length < 1) {
            System.err.println("Please insert this GS ID");
            return;
        }
        int id = Integer.parseInt(args[0]);

        RmiServer server = new RmiServer(1099);

        try {
            new GridScheduler(server, id, Repositories.resourceManagerRepository(), Repositories.gridSchedulerRepository());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
