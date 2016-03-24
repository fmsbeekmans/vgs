package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.model.cluster.EagerResourceManager;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws IOException, URISyntaxException {
        RmiServer server = new RmiServer(1099);
        URL url = UserMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManagerDriver> repo = Repository.fromFile(rmRepositoryFilePath);

        Map<Integer, String> rmUrls = repo.urls();
        for(int id : rmUrls.keySet()) {
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000);
            server.register(rmUrls.get(id), rmImpl);
        }

        while (true) {}
    }
}
