package santiagoAndFerdy.vgs.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.IDGen;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/20/16.
 */
public class User extends UnicastRemoteObject implements IUser {

    private static final long             serialVersionUID = 4963157568070156360L;
    private PrintWriter                   writer;
    private RmiServer                     rmiServer;
    private int                           id;
    private String                        url;
    private IRepository<IUser>            userRepository;
    private IRepository<IResourceManager> resourceManagerRepository;
    private Set<Job>                      pendingJobs;

    public User(RmiServer rmiServer, int id, IRepository<IUser> userRepository, IRepository<IResourceManager> resourceManagerRepository)
            throws RemoteException {

        this.rmiServer = rmiServer;
        this.id = id;
        this.userRepository = userRepository;
        this.resourceManagerRepository = resourceManagerRepository;

        this.url = userRepository.getUrl(id);
        pendingJobs = new HashSet<>();
        try {
            writer = new PrintWriter(new File("jobTimes" + id + ".csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // register self
        rmiServer.register(url, this);
    }

    /**
     * Method to create jobs and send them to the cluster selected
     * 
     * @param rmId
     *            - id of the destination cluster.
     * @param numJobs
     *            - number of jobs to be launched
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    @Override
    public void createJobs(int rmId, int numJobs, int duration) throws RemoteException {
        for (int i = 0; i < numJobs; i++) {
            final Job j = new Job(duration, IDGen.getNewId(), rmId);
            resourceManagerRepository.getEntity(rmId).ifPresent(resourceManager -> {
                pendingJobs.add(j);
                WorkRequest req = new WorkRequest(id, j);
                try {
                    System.out.println("[U\t" + id + "] Offering job " + j.getJobId() + " to rm " + rmId);
                    resourceManager.offerWork(req);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void acceptResult(Job j) throws RemoteException {
        System.out.println("[U\t" + id + "] Job " + j.getJobId() + " finished execution");
        String time = ((Long) (System.currentTimeMillis() - j.getStartTime())).toString();
        writer.write(j.getJobId() + "," + time + "\n");
        writer.flush();
        pendingJobs.remove(j);
        if (pendingJobs.isEmpty()) {
            System.out.println("[U\t" + id + "] All jobs completed");
        }
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public long ping() throws RemoteException {
        return 0;
    }
}
