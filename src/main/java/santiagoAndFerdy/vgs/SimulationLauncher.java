package santiagoAndFerdy.vgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.ArrayList;

import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

public class SimulationLauncher {
    final static ArrayList<Process> gsProcess = new ArrayList<>();
    final static ArrayList<Process> rmProcess = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        ProcessBuilder pb;
        for (int gsId : Repositories.GRID_SCHEDULER_REPOSITORY.ids()) {
            pb = new ProcessBuilder("java", "-jar", "GridScheduler.jar", Integer.toString(gsId));
            pb.redirectErrorStream(true);
            gsProcess.add(pb.start());
        }
        // IGridScheduler gs0 = new GridScheduler(rmiServer, 0, Repositories.RESOURCE_MANAGER_REPOSITORY, Repositories.GRID_SCHEDULER_REPOSITORY);
        // IGridScheduler gs1 = new GridScheduler(rmiServer, 1, Repositories.RESOURCE_MANAGER_REPOSITORY, Repositories.GRID_SCHEDULER_REPOSITORY);
        for (int rmId : Repositories.RESOURCE_MANAGER_REPOSITORY.ids()) {
            pb = new ProcessBuilder("java", "-jar", "ResourceManager.jar", Integer.toString(rmId), Integer.toString(2));
            pb.redirectErrorStream(true);
            rmProcess.add(pb.start());
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

        while (true) {

        }
        // IResourceManager rm0 = new ResourceManager(rmiServer, 0, Repositories.USER_REPOSITORY, Repositories.RESOURCE_MANAGER_REPOSITORY,
        // Repositories.GRID_SCHEDULER_REPOSITORY, 1);

        // User u0 = new User(rmiServer, 0, Repositories.USER_REPOSITORY, Repositories.RESOURCE_MANAGER_REPOSITORY);

        // u0.createJobs(0, 5);
    }
}
