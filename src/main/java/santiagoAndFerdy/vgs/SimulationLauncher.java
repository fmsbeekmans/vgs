package santiagoAndFerdy.vgs;

import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.ResourceManager;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

public class SimulationLauncher {
    public static void main(String[] args) throws RemoteException, InterruptedException {

        RmiServer rmiServer = new RmiServer(1099);

        Repositories.gridSchedulerRepository.ids().forEach(gsId -> {
            try {
                new GridScheduler(
                        rmiServer,
                        gsId,
                        Repositories.resourceManagerRepository,
                        Repositories.gridSchedulerRepository);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        Repositories.resourceManagerRepository.ids().forEach(rmId -> {
            try {
                new ResourceManager(
                        rmiServer,
                        rmId,
                        Repositories.userRepository,
                        Repositories.resourceManagerRepository,
                        Repositories.gridSchedulerRepository,
                        1000);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        Repositories.userRepository.ids().forEach(uId -> {
            try {
                new User(
                        rmiServer,
                        uId,
                        Repositories.userRepository,
                        Repositories.resourceManagerRepository);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        Repositories.userRepository.ids().forEach(uId -> {
            Repositories.userRepository.getEntity(uId).ifPresent(u -> {
                Repositories.resourceManagerRepository.ids().forEach(rmId -> {
                    try {
                        u.createJobs(rmId, 1, 1000);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }
}
