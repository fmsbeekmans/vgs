package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

import santiagoAndFerdy.vgs.discovery.IHeartbeatSender;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {
    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {
        RmiServer server = new RmiServer(1099);
        URL url = GridSchedulerMain.class.getClassLoader().getResource("rm/rms-hb");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IHeartbeatSender> repo = Repository.fromFile(rmRepositoryFilePath);
        GridScheduler gs = new GridScheduler(server, repo, "//localhost/gs-d0");
        server.register("//localhost/gs-d0", gs);
        //Thread.sleep(5000);
        //server.unRegister("//localhost/gs-d0-hb");
        //server.unRegister("//localhost/gs-d0");
        while(true){
            Thread.sleep(2000);
            gs.checkConnections();
            System.out.println("");
            
        }
    }
}
