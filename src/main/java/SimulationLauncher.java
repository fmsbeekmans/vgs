import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import santiagoAndFerdy.vgs.GridSchedulerMain;
import santiagoAndFerdy.vgs.ResourceManagerMain;
import santiagoAndFerdy.vgs.discovery.IHeartbeatSender;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerDriver;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class SimulationLauncher implements Runnable {
    private RmiServer      server;
    GridScheduler[]        gsArray;
    EagerResourceManager[] rmArray;

    public SimulationLauncher() throws IOException, URISyntaxException {
        server = new RmiServer(1099);

        // GS
        // RM HB repository
        URL url_rm_hb = GridSchedulerMain.class.getClassLoader().getResource("rm/rms-hb");
        Path rm_hb_RepositoryFilePath = Paths.get(url_rm_hb.toURI());
        IRepository<IHeartbeatSender> repoGS = Repository.fromFile(rm_hb_RepositoryFilePath);
        //
        gsArray = new GridScheduler[1];
        GridScheduler gs = new GridScheduler(server, repoGS, "//localhost/gs-d0");
        gsArray[0] = gs;
        server.register("//localhost/gs-d0", gs);
        

        // RM
        // RM urls
        URL urlRM = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(urlRM.toURI());
        IRepository<IResourceManagerDriver> repoRM = Repository.fromFile(rmRepositoryFilePath);
        Map<Integer, String> rmUrls = repoRM.urls();

        HashMap<Integer, EagerResourceManager> rmConnections = new HashMap<Integer, EagerResourceManager>();
        URL url_gs_hb = ResourceManagerMain.class.getClassLoader().getResource("gs/gss-hb");
        Path gs_hb_RepositoryFilePathHB = Paths.get(url_gs_hb.toURI());
        IRepository<IHeartbeatSender> repoGS_HB = Repository.fromFile(gs_hb_RepositoryFilePathHB);

        rmArray = new EagerResourceManager[2];
        for (int id : rmUrls.keySet()) {
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, rmUrls.get(id), repoGS_HB);
            rmArray[id] = rmImpl;
            server.register(rmUrls.get(id), rmImpl);
            rmImpl.startHBHandler();
            rmConnections.put(id, rmImpl);
        }
        gs.startHBHandler();
        // Run the simulation
        Thread runThread = new Thread(this);
        runThread.run(); // This method only returns after the simulation has ended
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        SimulationLauncher sim = new SimulationLauncher();

    }

    @Override
    public void run() {
        gsArray[0].checkConnections();
        
        rmArray[0].shutdown();
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            gsArray[0].checkConnections();

        }

    }

}
