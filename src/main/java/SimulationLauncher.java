import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import santiagoAndFerdy.vgs.ResourceManagerMain;
import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class SimulationLauncher implements Runnable {

    RmiServer                       server;
    GridScheduler[]                 gsArray;
    EagerResourceManager[]          rmArray;
    IRepository<IHeartbeatReceiver> repoGS;
    final ArrayList<Process>        gsProcesses = new ArrayList<Process>();
    final ArrayList<Process>        rmProcesses = new ArrayList<Process>();

    /**
     * Test to show that the HB protocol is working properly. It creates two RM and one GS. The simulation method(which in real would be the run of a
     * Thread) actually runs the simulation. Shows the status, kills one RM and then subsequently it creates it again the status is updated correctly.
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public SimulationLauncher() throws IOException, URISyntaxException {
        server = new RmiServer(1099);
        rmArray = new EagerResourceManager[2];
        gsArray = new GridScheduler[1];
        HashMap<Integer, EagerResourceManager> rmConnections = new HashMap<Integer, EagerResourceManager>();
        // RM
        URL urlRM = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(urlRM.toURI());
        IRepository<IHeartbeatReceiver> repoRM = Repository.fromFile(rmRepositoryFilePath);
        Map<Integer, String> rmUrls = repoRM.urls();

        // GS
        URL urlGS = ResourceManagerMain.class.getClassLoader().getResource("gs/gss");
        Path gsRepositoryFilePath = Paths.get(urlGS.toURI());
        repoGS = Repository.fromFile(gsRepositoryFilePath);
        Map<Integer, String> gsUrls = repoGS.urls();

        for (int id : gsUrls.keySet()) {
            String url = gsUrls.get(id);
            GridScheduler gs = new GridScheduler(server, repoRM, url);
            server.register(url, gs);
            gsArray[id] = gs;
        }

        for (int id : rmUrls.keySet()) {
            String url2 = rmUrls.get(id);
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, url2, repoGS);
            rmArray[id] = rmImpl;
            server.register(url2, rmImpl);
            rmConnections.put(id, rmImpl);
        }

        simulation(); // This actually should be the start of a Thread but since we are not going to use I just put it like this to show you
    }

    // WORKS!
    public SimulationLauncher(int asd) throws IOException, URISyntaxException {

        ProcessBuilder pb;

        // Shutting down hooks to processes
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down GS");
                for (Process p : gsProcesses)
                    p.destroy();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down RM");
                for (Process p : rmProcesses)
                    p.destroy();
            }
        });
        System.out.println("Let's start");

        // Create the GS nodes
        URL urlGS = SimulationLauncher.class.getClassLoader().getResource("gs/gss");
        Path gsRepositoryFilePath = Paths.get(urlGS.toURI());
        repoGS = Repository.fromFile(gsRepositoryFilePath);
        Map<Integer, String> gsUrls = repoGS.urls();

        for (int id : gsUrls.keySet()) {
            String url = gsUrls.get(id); //the filepath is hardcoded because I was getting exceptions...
            pb = new ProcessBuilder("java", "-jar", "GridScheduler.jar", url, "C:/Users/Santi/workspace/vgs/rm/rms");
            pb.redirectErrorStream(true);
            try {
                gsProcesses.add(pb.start());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Check what the GS nodes are printing...
        for (Process p : gsProcesses) {
            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            final BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            Thread t = new Thread() {
                BufferedReader bf = bufferedreader;

                public void run() {
                    String line;
                    try {
                        while ((line = bf.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                    }
                }
            };
            t.start();
        }
        // Create RM nodes
        URL urlRM = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(urlRM.toURI());
        IRepository<IHeartbeatReceiver> repoRM = Repository.fromFile(rmRepositoryFilePath);
        Map<Integer, String> rmUrls = repoRM.urls();

        for (int id : rmUrls.keySet()) {
            String url = rmUrls.get(id);
            pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", url, Integer.toString(id), "C:/Users/Santi/workspace/vgs/gs/gss");
            pb.redirectErrorStream(true);
            try {
                rmProcesses.add(pb.start());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Check what the RM nodes are printing...
        for (Process p : rmProcesses) {
            InputStream inputstream = p.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            final BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            Thread t = new Thread() {
                BufferedReader bf = bufferedreader;

                public void run() {
                    String line;
                    try {
                        while ((line = bf.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                    }
                }
            };
            t.start();
        }
        // Run the simulation
        Thread runThread = new Thread(this);
        runThread.run();

    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        new SimulationLauncher();

    }

    public void shutDown() {
        for (Process p : gsProcesses) {
            p.destroy();
        }
        for (Process p : rmProcesses) {
            p.destroy();
        }
    }

    /**
     * Run the simulation for 20s and then destroy the threads. I think we should figure out a better way of shutting down...
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            shutDown();
        }

    }

    public void simulation() {
        try {
            Thread.sleep(1000); // wait a little bit
            gsArray[0].checkConnections(); // check the status
            Thread.sleep(2000);
            rmArray[0].shutdown(); // kill one RM
        } catch (InterruptedException e) {
        }

        boolean restart = false;
        while (true) {
            try {
                Thread.sleep(2000);
                System.out.println("");
                gsArray[0].checkConnections(); // check status
            } catch (InterruptedException e) {
            }

            if (!restart) { // restart the node
                restart = true;
                try {
                    EagerResourceManager rmImpl = new EagerResourceManager(0, 10000, server, "//localhost/rm-d0", repoGS);
                    server.register("//localhost/rm-d0", rmImpl);
                    System.out.println("Restarted");
                } catch (Exception e) {
                }
            }

        }
    }

}
