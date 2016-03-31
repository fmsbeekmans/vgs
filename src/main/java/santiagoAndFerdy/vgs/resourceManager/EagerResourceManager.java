package santiagoAndFerdy.vgs.resourceManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;

import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.model.Request;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManagerDriver, IHeartbeatReceiver {

    private static final long               serialVersionUID = -4089353922882117112L;
    private Queue<Request>                  jobQueue;
    private Queue<Node>                     idleNodes;
    private int                             n;
    private int                             id;
    private long                            load;
    private HeartbeatHandler                hHandler;
    private ScheduledExecutorService        timerScheduler;
    private Engine                          engine;
    private HashMap<String, Task<Object>>   status;
    private RmiServer                       rmiServer;
    private String                          myURL;

    /**
     * Creates a RM
     * 
     * @param id
     *            - id of this RM
     * @param n
     *            - number of nodes it has
     * @param rmiServer
     *            - server to disconnect this RM later on
     * @param url
     *            - the RM url
     * @param repo
     *            - repository with the connections to GS it should have
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public EagerResourceManager(int id, int n, RmiServer rmiServer, String url, IRepository<IHeartbeatReceiver> repo)
            throws RemoteException, MalformedURLException {
        super();
        this.id = id;
        this.n = n;
        this.load = 0;
        this.rmiServer = rmiServer;
        status = new HashMap<String, Task<Object>>();
        // node queues synchronisation need the same mutex anyway. Don't use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(n);
        myURL = url;
        for (int i = 0; i < n; i++)
            idleNodes.add(new Node(i, this));

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        hHandler = new HeartbeatHandler(repo);

    }

    @Override
    public synchronized void queue(Request req) throws RemoteException, MalformedURLException, NotBoundException {
        jobQueue.add(req);
        System.out.println("Received job " + req.getJob().getJobId());
        load += req.getJob().getDuration();

        Task<Void> process = Task.action(() -> processQueue());
        engine.run(process);
    }

    @Override
    public void respond(Request req) throws RemoteException, MalformedURLException, NotBoundException {
        Task<Void> respond = Task.action(() -> {
            IResourceManagerUserClient client = (IResourceManagerUserClient) Naming.lookup(req.getUser().getUrl());
            client.acceptResult(req.getJob());
            release(req);
        });

        engine.run(respond);
    }

    private synchronized void release(Request req) {
        load -= req.getJob().getDuration();
    }

    @Override
    public synchronized void finish(Node node, Request req) throws RemoteException, NotBoundException, MalformedURLException {
        respond(req);
        idleNodes.add(node);

        Task<Void> process = Task.action(() -> processQueue());
        engine.run(process);
    }

    // should be run after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while (!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            Request req = jobQueue.poll();
            System.out.println("Running job " + req.getJob().getJobId());

            Task<Void> run = Task.action(() -> allocatedNode.handle(req));
            engine.run(run);
        }
    }

    @Override
    public ScheduledExecutorService executorService() throws RemoteException {
        return timerScheduler;
    }
    /**
     * public method to check the connections the handler is monitoring
     */
    public void checkConnections() {
        hHandler.checkLife();
    }
    /**
     * Shutdown method to kill this RM
     */
    public void shutdown() {
        try {
            rmiServer.unRegister(myURL);
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Method to check if this RM is alive. Nothing is needed 
     */
    @Override
    public void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException {}
}
