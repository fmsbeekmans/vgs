package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {
        if (args.length < 2) {
            System.err.println("Please insert job duration (ms) and User ID, optional number of jobs (default 100)");
            return;
        }

        int jobDuration = Integer.parseInt(args[0]);
        int id = Integer.parseInt(args[1]);
        int numJobs;
        if (args.length == 3) {
            numJobs = Integer.parseInt(args[2]);
        } else
            numJobs = 100;
        RmiServer rmiServer = new RmiServer(1099);

        // new User(rmiServer, id, Repositories.userRepository, Repositories.resourceManagerRepository).createJobs(toRM, numJobs, jobDuration);
        Repositories.userRepository().ids().forEach(uId -> {
            if (uId == id) {
                try {
                    new User(rmiServer, uId, Repositories.userRepository(), Repositories.resourceManagerRepository());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        Repositories.userRepository().ids().forEach(uId -> {
            try {
                Repositories.userRepository().getEntity(uId).ifPresent(u -> {
                    if (uId == id) {
                        try {
                            Repositories.resourceManagerRepository().ids().forEach(rmId -> {

                                try {
                                    u.createJobs(rmId, numJobs, jobDuration);
                                } catch (Exception e) {
                                    e.printStackTrace();

                                }

                            });
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

    }
}