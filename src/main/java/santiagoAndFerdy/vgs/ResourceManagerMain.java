package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerDriver;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.util.Map;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
        RmiServer server = new RmiServer(1099);
        URL url = UserMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManagerDriver> rmRepo = Repository.fromFile(rmRepositoryFilePath);
        url = UserMain.class.getClassLoader().getResource("rm/gss");
        Path gsRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IGridSchedulerResourceManagerClient> gsRepo = Repository.fromFile(gsRepositoryFilePath);

        Map<Integer, String> rmUrls = rmRepo.urls();
        for(int id : rmUrls.keySet()) {
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, rmUrls.get(id), server, gsRepo);
            server.register(rmUrls.get(0), rmImpl);
        }
    }
}