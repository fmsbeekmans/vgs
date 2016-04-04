package santiagoAndFerdy.vgs;

import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

public class SimulationLauncher {
    public static void main(String[] args) throws RemoteException, InterruptedException {
        RmiServer rmiServer = new RmiServer(1099);

        IGridScheduler gs0 = new GridScheduler(
                rmiServer,
                0,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                Repositories.GRID_SCHEDULER_REPOSITORY);
        IGridScheduler gs1 = new GridScheduler(
                rmiServer,
                1,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                Repositories.GRID_SCHEDULER_REPOSITORY);

        IResourceManager rm0 = new ResourceManager(
                rmiServer,
                0,
                Repositories.USER_REPOSITORY,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                Repositories.GRID_SCHEDULER_REPOSITORY,
                4);

        User u0 = new User(
                rmiServer,
                0,
                Repositories.USER_REPOSITORY,
                Repositories.RESOURCE_MANAGER_REPOSITORY);

        u0.createJobs(0, 50);
    }
}
