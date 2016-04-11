package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.rmi.RemoteException;

public class GridSchedulerCrash {
    public static void main(String[] args) throws RemoteException, InterruptedException {
        RmiServer rmiServer = new RmiServer(1099);

        try {
            IGridScheduler gs0 = new GridScheduler(
                    rmiServer,
                    0,
                    Repositories.resourceManagerRepository(),
                    Repositories.gridSchedulerRepository());
            IGridScheduler gs1 = new GridScheduler(
                    rmiServer,
                    1,
                    Repositories.resourceManagerRepository(),
                    Repositories.gridSchedulerRepository());
            IGridScheduler gs2 = new GridScheduler(
                    rmiServer,
                    2,
                    Repositories.resourceManagerRepository(),
                    Repositories.gridSchedulerRepository());

            IResourceManager rm0 = new ResourceManager(
                    rmiServer,
                    0,
                    Repositories.userRepository(),
                    Repositories.resourceManagerRepository(),
                    Repositories.gridSchedulerRepository(),
                    4);
            IResourceManager rm1 = new ResourceManager(
                    rmiServer,
                    1,
                    Repositories.userRepository(),
                    Repositories.resourceManagerRepository(),
                    Repositories.gridSchedulerRepository(),
                    4);


            User u0 = new User(
                    rmiServer,
                    0,
                    Repositories.userRepository(),
                    Repositories.resourceManagerRepository());

            User u1 = new User(
                    rmiServer,
                    1,
                    Repositories.userRepository(),
                    Repositories.resourceManagerRepository());

            u0.createJobs(0, 1, 5000);

            Thread.sleep(1000);

            gs0.shutDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
