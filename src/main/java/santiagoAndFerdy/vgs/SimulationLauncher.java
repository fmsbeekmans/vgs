package santiagoAndFerdy.vgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class SimulationLauncher implements Runnable {

    RmiServer                     server;
    HashMap <IGridScheduler,ArrayList<IResourceManager>> connections;
    //
    final ArrayList<Process>      gsProcesses = new ArrayList<Process>();
    final ArrayList<Process>      rmProcesses = new ArrayList<Process>();

    /**
     * TEST without executing components with their mains.
     * @throws IOException
     * @throws URISyntaxException
     * @throws NotBoundException
     */
    public SimulationLauncher() throws IOException, URISyntaxException, NotBoundException {
        server = new RmiServer(1099);
        connections = new HashMap<IGridScheduler,ArrayList<IResourceManager>>();


        // Get list of all GS
        URL urlGS = ResourceManagerMain.class.getClassLoader().getResource("gs/gss");
        Path gsRepositoryFilePath = Paths.get(urlGS.toURI());
        IRepository<IGridScheduler> gridSchedulerRepository = Repository.fromFile(gsRepositoryFilePath);

        //For each GS create the GS and all the RMs under its control
        //Each GS receives the connections of RMs under its control and the next and previous GS on the ring.
        
        for (int id : gridSchedulerRepository.ids()) {
            String url = gridSchedulerRepository.getUrl(id);
            URL urlGSConnections = GridSchedulerMain.class.getClassLoader().getResource("gs/gs"+id+"/gss");
            IRepository<IGridScheduler> repoGSBack = Repository.fromFile(Paths.get(urlGSConnections.toURI()));
            URL urlRMConnections = GridSchedulerMain.class.getClassLoader().getResource("gs/gs"+id+"/rms");
            IRepository<IResourceManager> repoRM = Repository.fromFile(Paths.get(urlRMConnections.toURI()));
            GridScheduler gs = new GridScheduler(server, repoRM, repoGSBack, url, id);
            
            ArrayList <IResourceManager> rms = new ArrayList<IResourceManager>();
            for(int idRM : repoRM.ids()){ //Each RM receives the connections to the main and backup GS.
                String RMURL = repoRM.getUrl(idRM);
                URL urlRMGSConnections = GridSchedulerMain.class.getClassLoader().getResource("rm/gs"+id+"/gss");
                IRepository<IGridScheduler> repoGS = Repository.fromFile(Paths.get(urlRMGSConnections.toURI()));
                EagerResourceManager rmImpl = new EagerResourceManager(idRM, 10000, server, RMURL, repoGS);
                server.register(RMURL, rmImpl);
                rms.add(rmImpl);
            }
            connections.put(gs, rms);
        }
        simulation(); //Run simulation
    }

    /**
     * Method which launches 2 RMs and 1 GS from their own main (jar files are needed). Then it creates a Thread which runs the simulation for 200s
     * The components access the URLs by connecting to an S3 bucket in AWS. It take sa long time to start because of accessing the S3 bucket (dunno
     * why) The simulation then kills one RM by calling the shutdown mething via the interface IRemoteShutdown and hen it restarts it again.
     * 
     * @param asd
     *            - nothing
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
//    public SimulationLauncher(int asd) throws IOException, URISyntaxException, InterruptedException {
//
//        ProcessBuilder pb;
//
//        // Shutting down hooks to processes
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                System.out.println("Shutting down GS");
//                for (Process p : gsProcesses)
//                    p.destroy();
//            }
//        });
//
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                System.out.println("Shutting down RM");
//                for (Process p : rmProcesses)
//                    p.destroy();
//            }
//        });
//        System.out.println("Let's start");
//
//        // Create the GS nodes
//        URL urlGS = SimulationLauncher.class.getClassLoader().getResource("gs/gss");
//        Path gsRepositoryFilePath = Paths.get(urlGS.toURI());
//        gridSchedulerRepository = Repository.fromFile(gsRepositoryFilePath);
//
//        for (int id : gridSchedulerRepository.ids()) {
//            String url = gridSchedulerRepository.getUrl(id);
//            pb = new ProcessBuilder("java", "-jar", "GridScheduler.jar", Integer.toString(id), url, "vgs-repository", "gs/rms", "gs/gss");
//            pb.redirectErrorStream(true);
//            try {
//                gsProcesses.add(pb.start());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // Check what the GS nodes are printing...
//        for (Process p : gsProcesses) {
//            InputStream inputstream = p.getInputStream();
//            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
//            final BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
//            Thread t = new Thread() {
//                BufferedReader bf = bufferedreader;
//
//                public void run() {
//                    String line;
//                    try {
//                        while ((line = bf.readLine()) != null) {
//                            System.out.println(line);
//                        }
//                    } catch (IOException e) {
//                    }
//                }
//            };
//            t.start();
//        }
//
//        // Create RM nodes
//        URL urlRM = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
//        Path rmRepositoryFilePath = Paths.get(urlRM.toURI());
//        resourceManagerRepository = Repository.fromFile(rmRepositoryFilePath);
//
//        for (int id : resourceManagerRepository.ids()) {
//            String url = resourceManagerRepository.getUrl(id);
//            pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", url, Integer.toString(id), "vgs-repository", "rm/gs-clients");
//            pb.redirectErrorStream(true);
//            try {
//                rmProcesses.add(pb.start());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        // Check what the RM nodes are printing...
//        for (Process p : rmProcesses) {
//            InputStream inputstream = p.getInputStream();
//            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
//            final BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
//            Thread t = new Thread() {
//                BufferedReader bf = bufferedreader;
//
//                public void run() {
//                    String line;
//                    try {
//                        while ((line = bf.readLine()) != null) {
//                            System.out.println(line);
//                        }
//                    } catch (IOException e) {
//                    }
//                }
//            };
//            t.start();
//        }
//        // Run the simulation
//        Thread runThread = new Thread(this);
//        runThread.run();
//
//    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException, NotBoundException {
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
        boolean restart = false;
        try {
            // Let all the nodes start... It takes a long time
            Thread.sleep(70000);
        } catch (InterruptedException e1) {
        }
        while (true) {
//            try {
//                // Run the simulation for 200s
//                Thread.sleep(200000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            shutDown();
        }

    }

    public void simulation() {
//        try {
//            Thread.sleep(1000); // wait a little bit
//            gsArray[0].checkConnections(); // check the status
//            Thread.sleep(2000);
//            IRemoteShutdown driver; // Shutdown the RM 0
//            driver = (IRemoteShutdown) Naming.lookup(urlRM0);
//            driver.shutDown();
//        } catch (InterruptedException | MalformedURLException | RemoteException | NotBoundException e) {
//        }

        boolean restart = false;
        while (true) {
//            try {
//                Thread.sleep(4000);
//                System.out.println("");
//                gsArray[0].checkConnections(); // check status
//            } catch (InterruptedException e) {
//            }
//
//            if (!restart) { // restart the node
//                restart = true;
//                try {
//                    EagerResourceManager rmImpl = new EagerResourceManager(0, 10000, server, urlRM0, gridSchedulerRepository);
//                    server.register(urlRM0, rmImpl);
//                    System.out.println(urlRM0 + " restarted");
//                } catch (Exception e) {
//                }
//            }

        }
    }

}
