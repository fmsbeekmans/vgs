package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.rmi.RemoteException;

public class GridSchedulerCrash {
    public static void main(String[] args) throws RemoteException, InterruptedException {
        RmiServer rmiServer = new RmiServer(1099);

        IGridScheduler gs0 = new GridScheduler(
                rmiServer,
                0,
                IRepository.Repositories.resourceManagerRepository,
                IRepository.Repositories.gridSchedulerRepository);
        IGridScheduler gs1 = new GridScheduler(
                rmiServer,
                1,
                IRepository.Repositories.resourceManagerRepository,
                IRepository.Repositories.gridSchedulerRepository);
        IGridScheduler gs2 = new GridScheduler(
                rmiServer,
                2,
                IRepository.Repositories.resourceManagerRepository,
                IRepository.Repositories.gridSchedulerRepository);

        IResourceManager rm0 = new ResourceManager(
                rmiServer,
                0,
                IRepository.Repositories.userRepository,
                IRepository.Repositories.resourceManagerRepository,
                IRepository.Repositories.gridSchedulerRepository,
                4);
        IResourceManager rm1 = new ResourceManager(
                rmiServer,
                1,
                IRepository.Repositories.userRepository,
                IRepository.Repositories.resourceManagerRepository,
                IRepository.Repositories.gridSchedulerRepository,
                4);

        User u0 = new User(
                rmiServer,
                0,
                IRepository.Repositories.userRepository,
                IRepository.Repositories.resourceManagerRepository);

        User u1 = new User(
                rmiServer,
                1,
                IRepository.Repositories.userRepository,
                IRepository.Repositories.resourceManagerRepository);

        u0.createJobs(0, 5);

        Thread.sleep(1000);

        gs0.shutDown();
    }
}
