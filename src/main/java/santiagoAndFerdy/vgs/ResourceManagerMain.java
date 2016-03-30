package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerDriver;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
        RmiServer rmiServer = new RmiServer(1099);
        URL rmFileUrl = UserMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(rmFileUrl.toURI());
        IRepository<IResourceManagerDriver> rmRepo = Repository.fromFile(rmRepositoryFilePath);
        Map<Integer, String> rmUrls = rmRepo.urls();

        URL gsClientFileUrl = UserMain.class.getClassLoader().getResource("rm/gs-clients");
        Path gsClientRepositoryFilePath = Paths.get(gsClientFileUrl.toURI());
        IRepository<IGridSchedulerResourceManagerClient> gsClientRepo = Repository.fromFile(gsClientRepositoryFilePath);
        Map<Integer, String> gsClientUrls = gsClientRepo.urls();
        Map<Integer, IGridSchedulerResourceManagerClient> gsClients = new HashMap<>();

        URL gsDriverFileUrl = UserMain.class.getClassLoader().getResource("rm/gs-drivers");
        Path gsDriverRepositoryFilePath = Paths.get(gsDriverFileUrl.toURI());
        IRepository<IGridSchedulerResourceManagerClient> gsDriverRepo = Repository.fromFile(gsDriverRepositoryFilePath);
        Map<Integer, String> gsDriverUrls = gsDriverRepo.urls();

        // create and bind clients
        for (int gsClientId : gsClientUrls.keySet()) {
            String clientUrl =
                    gsClientUrls.get(gsClientId);
            IGridSchedulerResourceManagerClient gsClient = new GridSchedulerResourceManagerClient(
                    rmiServer,
                    gsClientId,
                    clientUrl,
                    gsDriverUrls.get(gsClientId));
            gsClients.put(gsClientId, gsClient);
            rmiServer.register(clientUrl, gsClient);
        }


        for(int id : rmUrls.keySet()) {
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, rmUrls.get(id), rmiServer, gsClients);
            rmiServer.register(rmUrls.get(id), rmImpl);
        }

        System.out.println("Waiting for work");

        while (true);
    }
}