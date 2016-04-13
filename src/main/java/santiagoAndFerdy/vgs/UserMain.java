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
            System.err.println("Please insert job duration (ms), User ID, flag regular, overload or offload, optional number of jobs (default 100)");
            return;
        }

        int jobDuration = Integer.parseInt(args[0]);
        int id = Integer.parseInt(args[1]);
        int numJobs;
        String flag = args[2];
        if (args.length == 4) {
            numJobs = Integer.parseInt(args[3]);
        } else
            numJobs = 100;
        RmiServer rmiServer = new RmiServer(1099);

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
        // REGULAR, send numJobs to every RM
        if (flag.equals("regular")) {
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
        } else if (flag.equals("overload")) { // Overload a certain RM to force to load balance
            Repositories.userRepository().ids().forEach(uId -> {
                try {
                    Repositories.userRepository().getEntity(uId).ifPresent(u -> {
                        if (uId == id) {
                            try {
                                Repositories.resourceManagerRepository().ids().forEach(rmId -> {
                                    if (rmId == 0) { // send 800 jobs to RM 0
                                        try {
                                            u.createJobs(rmId, 800, jobDuration);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else { // send 200 to the rest
                                        try {
                                            u.createJobs(rmId, 200, jobDuration);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else { // Offload Test send 10 jobs to 3 RM, they will offload on the GS and the GS will allocate them in a new one
            Repositories.userRepository().ids().forEach(uId -> {
                try {
                    Repositories.userRepository().getEntity(uId).ifPresent(u -> {
                        if (uId == id) {
                            try {
                                Repositories.resourceManagerRepository().ids().forEach(rmId -> {
                                    if (rmId < 2) { // send just 10 jobs to 2 RMs
                                        try {
                                            u.createJobs(rmId, 10, jobDuration);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
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
}