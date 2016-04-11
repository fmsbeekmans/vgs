package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.FileNotFoundException;
import java.rmi.RemoteException;

public class ResourceManagerCrash {
    public static void main(String[] args) throws RemoteException, InterruptedException, FileNotFoundException {
        RmiServer rmiServer = new RmiServer(1099);

        IGridScheduler gs0 = new GridScheduler(
                rmiServer,
                0,
                Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository);
        IGridScheduler gs1 = new GridScheduler(
                rmiServer,
                1,
                Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository);
        IGridScheduler gs2 = new GridScheduler(
                rmiServer,
                2,
                Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository);

        IResourceManager rm0 = new ResourceManager(
                rmiServer,
                0,
                Repositories.userRepository,
                Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository,
                1);
        IResourceManager rm1 = new ResourceManager(
                rmiServer,
                1,
                Repositories.userRepository,
                Repositories.resourceManagerRepository,
                Repositories.gridSchedulerRepository,
                1);

        User u0 = new User(
                rmiServer,
                0,
                Repositories.userRepository,
                Repositories.resourceManagerRepository);

        User u1 = new User(
                rmiServer,
                1,
                Repositories.userRepository,
                Repositories.resourceManagerRepository);

        u0.createJobs(0, 3, 5000);

        Thread.sleep(1000);

        rm0.shutDown();
    }
}
