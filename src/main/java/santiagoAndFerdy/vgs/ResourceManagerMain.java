package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IHeartbeatSender;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerDriver;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        RmiServer server = new RmiServer(1099);
        URL url = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManagerDriver> repo = Repository.fromFile(rmRepositoryFilePath);
        Map<Integer, String> rmUrls = repo.urls();
        HashMap<Integer, EagerResourceManager> rmConnections = new HashMap<Integer, EagerResourceManager>();
        URL urlHB = ResourceManagerMain.class.getClassLoader().getResource("gs/gss-hb");
        Path rmRepositoryFilePathHB = Paths.get(urlHB.toURI());
        IRepository<IHeartbeatSender> repoHB = Repository.fromFile(rmRepositoryFilePathHB);

        for (int id : rmUrls.keySet()) {
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, rmUrls.get(id), repoHB);
            server.register(rmUrls.get(id), rmImpl);
            rmConnections.put(id, rmImpl);
        }
        
        Thread.sleep(5000);
        rmConnections.get(0).shutdown();
        
       //for(int i : rmUrls.keySet()){
            //server.lookUp("");
        //}
        while (true) {
            
        }
    }
}
