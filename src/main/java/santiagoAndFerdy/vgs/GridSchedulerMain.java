package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.util.Map;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridSchedulerDriver;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerGridSchedulerClient;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {
    public static void main(String[] args) throws IOException, InterruptedException, NotBoundException, URISyntaxException {
        RmiServer rmiServer = new RmiServer(1099);

        URL url = UserMain.class.getClassLoader().getResource("gs/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManagerGridSchedulerClient> rmRepository = Repository.fromFile(rmRepositoryFilePath);
        url = UserMain.class.getClassLoader().getResource("gs/gss");
        Path gsRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IGridSchedulerGridSchedulerClient> gsRepository = Repository.fromFile(gsRepositoryFilePath);
        Map<Integer, String> gridSchedulerUrls = gsRepository.urls();

        for(int gsId : gsRepository.ids()) {
            GridSchedulerDriver driver = new GridSchedulerDriver(
                    rmiServer,
                    rmRepository,
                    gsRepository,
                    gridSchedulerUrls.get(gsId),
                    gsId);
        }

        System.out.println("Waiting for work");

        while (true);
    }
}
