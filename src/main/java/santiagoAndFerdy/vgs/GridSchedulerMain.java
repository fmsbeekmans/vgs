package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {

    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {

        RmiServer server = new RmiServer(1099);

        Repositories.gridSchedulerRepository().ids().forEach(gsId -> {
            try {
                new GridScheduler(server, gsId, Repositories.resourceManagerRepository(), Repositories.gridSchedulerRepository());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}