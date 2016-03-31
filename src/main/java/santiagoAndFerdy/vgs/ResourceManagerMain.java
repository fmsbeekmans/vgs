package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        if (args.length < 3) {
            System.err.println("Please enter the URL of this ResourceManager, the id and the location of GS registry");
            return;
        }

        // Probably will have to pass the number of nodes as a parameter as well...
        String url = args[0];
        int id = Integer.valueOf(args[1]);
        String registryLocation = args[2];

        // GS Repository for connections
        // Hardcoded because I am getting java.nio.file.FileSystemNotFoundException: Provider "rsrc" not installed, in the future it will be a path to
        // a S3 bucket in Amazon
        Path gsRepositoryFilePath = Paths.get(registryLocation);
        IRepository<IHeartbeatReceiver> repoGS = Repository.fromFile(gsRepositoryFilePath);

        RmiServer server = new RmiServer(1099);
        EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, url, repoGS);
        server.register(url, rmImpl);

    }
}
