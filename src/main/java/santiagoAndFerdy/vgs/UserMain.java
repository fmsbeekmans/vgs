package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.Repositories;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    private static Engine                   engine;
    private static ScheduledExecutorService timer;

    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {
        if (args.length < 2) {
            System.err.println("Please insert job duration (ms), User ID, flag regular, overload or offload, optional number of jobs (default 100)");
            return;
        }
        ExecutorService taskScheduler = Executors.newFixedThreadPool(100);
        timer = Executors.newSingleThreadScheduledExecutor();
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timer).build();

        // Params
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
                    e.printStackTrace();
                }
            }
        });
        // REGULAR, send numJobs to every RM at the same time as task.
        if (flag.equals("regular")) {
            Repositories.userRepository().ids().forEach(uId -> {
                try {
                    Repositories.userRepository().getEntity(uId).ifPresent(u -> {
                        if (uId == id) {
                            try {
                                Repositories.resourceManagerRepository().ids().forEach(rmId -> {
                                    engine.run(Task.action(() -> {
                                        try {
                                            u.createJobs(rmId, numJobs, jobDuration);
                                        } catch (Exception e) {
                                            e.printStackTrace();

                                        }
                                    }));
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
        } else if (flag.equals("offload")) { // Offload Test send 10 jobs to 3 RM, they will offload on the GS and the GS will allocate them in a new
                                             // one according with the load balancing policy implemented on the selectors.
            Repositories.userRepository().ids().forEach(uId -> {
                try {
                    Repositories.userRepository().getEntity(uId).ifPresent(u -> {
                        if (uId == id) {
                            try {
                                Repositories.resourceManagerRepository().ids().forEach(rmId -> {
                                    if (rmId < 2) { // send just 10 jobs to 2 first RMs (easy with the logging)
                                        try {
                                            u.createJobs(rmId, 10, jobDuration);
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
        } else { // send jobs sequentially (first to RM0, then to RM1...)
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
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}