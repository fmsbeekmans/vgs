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
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridSchedulerDriver;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

public class SimulationLauncher implements Runnable {

    RmiServer                       server;
    Map<Integer, String>            rmUrls;
    Map<Integer, String>            gsUrls;
    IRepository<IHeartbeatReceiver> repoGS;
    GridSchedulerDriver[]           gsArray;
    //
    final ArrayList<Process>        gsProcesses = new ArrayList<Process>();
    final ArrayList<Process>        rmProcesses = new ArrayList<Process>();

    /**
     * JUST FOR TEST WITHOUT MAIN Test to show that the HB protocol is working properly. It creates two RM and one GS. The simulation method(which in
     * real would be the run of a Thread) actually runs the simulation. Shows the status, kills one RM and then subsequently it creates it again the
     * status is updated correctly.
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public SimulationLauncher() throws IOException, URISyntaxException {
        server = new RmiServer(1099);

        // RM
        URL urlRM = ResourceManagerMain.class.getClassLoader().getResource("rm/rms");
        Path rmRepositoryFilePath = Paths.get(urlRM.toURI());
        IRepository<IHeartbeatReceiver> repoRM = Repository.fromFile(rmRepositoryFilePath);
        rmUrls = repoRM.urls();

        // GS
        URL urlGS = ResourceManagerMain.class.getClassLoader().getResource("gs/gss");
        Path gsRepositoryFilePath = Paths.get(urlGS.toURI());
        repoGS = Repository.fromFile(gsRepositoryFilePath);
        Map<Integer, String> gsUrls = repoGS.urls();
        gsArray = new GridSchedulerDriver[gsUrls.size()];
        for (int id : gsUrls.keySet()) {
            String url = gsUrls.get(id);
            GridSchedulerDriver gs = new GridSchedulerDriver(server, repoRM, url);
            server.register(url, gs);
            gsArray[id] = gs;
        }

        for (int id : rmUrls.keySet()) {
            String url2 = rmUrls.get(id);
            EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, url2, repoGS);

            server.register(url2, rmImpl);

        }

        simulation(); // This actually should be the start of a Thread but since we are not going to use I just put it like this to show you
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
    public SimulationLauncher(int asd) throws IOException, URISyntaxException, InterruptedException {

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
        gsUrls = repoGS.urls();

        for (int id : gsUrls.keySet()) {
            String url = gsUrls.get(id); // the filepath is hardcoded because I was getting exceptions...
            pb = new ProcessBuilder("java", "-jar", "GridScheduler.jar", url, "vgs-repository", "rm/rms");
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
        rmUrls = repoRM.urls();

        for (int id : rmUrls.keySet()) {
            String url = rmUrls.get(id);
            pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", url, Integer.toString(id), "vgs-repository", "gs/gss");
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

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        new SimulationLauncher(1);

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
            // Let all the nodes start... It ake sa long time
            Thread.sleep(70000);
        } catch (InterruptedException e1) {
        }
        while (true) {
            try {
                if (!restart) {
                    restart = true;
                    System.out.println("LET'S RESTART");
                    IRemoteShutdown driver;
                    driver = (IRemoteShutdown) Naming.lookup(rmUrls.get(0));
                    driver.shutDown();
                    // This shouldn't be needed to done manually...
                    rmProcesses.get(0).destroy();
                    rmProcesses.remove(0);
                    Thread.sleep(7000);
                    String url = rmUrls.get(0);
                    int id = 0;
                    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", url, Integer.toString(id), "vgs-repository",
                            "gs/gss");
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    rmProcesses.add(p);
                    // Get the output
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

                // Run the simulation for 200s
                Thread.sleep(200000);
            } catch (InterruptedException | NotBoundException | IOException e) {
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
            IRemoteShutdown driver;
            driver = (IRemoteShutdown) Naming.lookup(rmUrls.get(0));
            driver.shutDown();
        } catch (InterruptedException | MalformedURLException | RemoteException | NotBoundException e) {
        }

        boolean restart = false;
        while (true) {
            try {
                Thread.sleep(4000);
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
