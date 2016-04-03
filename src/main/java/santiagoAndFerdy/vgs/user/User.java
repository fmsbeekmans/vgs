package santiagoAndFerdy.vgs.user;

import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
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
public class User extends UnicastRemoteObject implements IUser, Runnable {

    private static final long serialVersionUID = 4963157568070156360L;
    private String                        url;
    private RmiServer                     server;
    private IRepository<IResourceManager> resourceManagerRepository;
    private Set<Job>                      pendingJobs;
    private Thread                        pollThread;

    public User(IRepository<IResourceManager> resourceManagerRepository, String url, RmiServer server) throws MalformedURLException, RemoteException {
        this.url = url;
        this.resourceManagerRepository = resourceManagerRepository;
        pendingJobs = new HashSet<>();
        this.server = server;
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
    public void createJobs(int rmId, int numJobs) throws RemoteException, NotBoundException, MalformedURLException {


        for (long i = 0; i < numJobs; i++) {
            final Job j = new Job(1000, IDGen.getNewId(), rmId);
            IResourceManager resourceManager = resourceManagerRepository.getEntity(rmId);
            pendingJobs.add(j);
            if (this.pollThread == null) {
                pollThread = new Thread(this);
                pollThread.start();
            }
            WorkRequest req = new WorkRequest(url, j);
            resourceManager.offerWork(req);
        }
    }

    @Override
    public void acceptResult(Job j) throws RemoteException {
        System.out.println("Job " + j.getJobId() + " finished execution");
        pendingJobs.remove(j);
    }

    @Override
    public void run() {
        while (!pendingJobs.isEmpty()) {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }
        // shutting down client
        try {
            server.unRegister(url);
            UnicastRemoteObject.unexportObject(this, true);
            pollThread.join();
        } catch (InterruptedException | NoSuchObjectException e) {
            e.printStackTrace();
        }
    }
}
