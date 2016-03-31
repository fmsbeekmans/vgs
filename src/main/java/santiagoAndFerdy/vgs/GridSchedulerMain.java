package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {
    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {
        if (args.length < 2) {
            System.err.println("Please enter the URL of this GridScheduler and the location of RM registry");
            return;
        }
        String url = args[0];
        String registryLocation = args[1];

        // RM Repository for connections

        // Hardcoded because I am getting java.nio.file.FileSystemNotFoundException: Provider "rsrc" not installed, in the future it will be a path to
        // a S3 bucket in Amazon
        Path rmRepositoryFilePath = Paths.get(registryLocation);
        System.out.println(rmRepositoryFilePath);
        IRepository<IHeartbeatReceiver> repoRM = Repository.fromFile(rmRepositoryFilePath);

        RmiServer server = new RmiServer(1099);

        GridScheduler gs = new GridScheduler(server, repoRM, url);
        server.register(url, gs);
        while(true){
            Thread.sleep(2000);
            gs.checkConnections();
        }

    }
}
