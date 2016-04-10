package santiagoAndFerdy.vgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

public class SimulationLauncherExternal {
    final static ArrayList<Process> gsProcess = new ArrayList<>();
    final static ArrayList<Process> rmProcess = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException, IOException {

        RmiServer rmiServer = new RmiServer(1099);
        ProcessBuilder pb;
        
        //GSs
        for (int gsId : Repositories.gridSchedulerRepository.ids()) {
            pb = new ProcessBuilder("java", "-jar", "GridScheduler.jar", Integer.toString(gsId));
            pb.redirectErrorStream(true);
            gsProcess.add(pb.start());
        }
        for (Process p : gsProcess) {
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
        
        
        //RMs
        for (int rmId : Repositories.resourceManagerRepository.ids()) {
            pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", Integer.toString(rmId));
            pb.redirectErrorStream(true);
            rmProcess.add(pb.start());
        }
        for (Process p : rmProcess) {
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
        
        
       //User u0 = new User(rmiServer, 0, Repositories.userRepository, Repositories.resourceManagerRepository);

        //User u1 = new User(rmiServer, 1, Repositories.userRepository, Repositories.resourceManagerRepository);

       // u0.createJobs(0, 1000, 100);
        // u0.createJobs(1, 1000, 200);
       // u1.createJobs(0, 1000, 50);
        // u1.createJobs(1, 1000, 10);
    }
}
