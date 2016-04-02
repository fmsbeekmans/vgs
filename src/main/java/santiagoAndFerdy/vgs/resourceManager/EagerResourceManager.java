package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.messages.MonitoringRequest;
import santiagoAndFerdy.vgs.messages.WorkRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.user.IUser;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManager, IRemoteShutdown {
    private static final long                                 serialVersionUID = -4089353922882117112L;

    private Queue<WorkRequest>                                jobQueue;
    private Queue<Node>                                       idleNodes;

    private RmiServer                                         rmiServer;
    private int                                               nNodes;
    private int                                               id;
    private String                                            url;

    private IRepository<IGridScheduler>  gridSchedulerRepository;
    private HeartbeatHandler                                  hHandler;
    private long                                              load;
    private ScheduledExecutorService                          timerScheduler;
    private Engine                                            engine;

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
     * @param gridSchedulerRepository
     *            - repository with the connections to GS it should have
     * @throws RemoteException
     * @throws MalformedURLException
     */
    public EagerResourceManager(int id, int n, RmiServer rmiServer, String url,
            IRepository<IGridScheduler> gridSchedulerRepository)
                    throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.id = id;
        this.url = url;
        this.nNodes = n;
        this.load = 0;
        // node queues synchronisation need the same mutex anyway. Don't use need to use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(nNodes);

        this.rmiServer = rmiServer;
        this.gridSchedulerRepository = gridSchedulerRepository;


        // I think that the RM should ping the GS not the client that he has... I think we could have the functionality of the client here inside the
        // RM.
        hHandler = new HeartbeatHandler(gridSchedulerRepository, id, false);
        for (int i = 0; i < nNodes; i++)
            idleNodes.add(new Node(i, this, timerScheduler));
        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
    }

    @Override
    public synchronized void offerWork(WorkRequest req) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Received job " + req.getToExecute().getJobId());

        Task<Void> queueFlow = Task
                // send to GS first
                .action(() -> requestMonitoring(req.getToExecute()))
                // then schedule
                .andThen(monitored -> jobQueue.add(req))
                // and process the queue again
                .andThen(queue -> processQueue());
        engine.run(queueFlow);
        load += req.getToExecute().getDuration();
    }

    @Override
    public void schedule(@NotNull WorkRequest toSchedule) throws RemoteException, MalformedURLException, NotBoundException {
        // Assume a job is already monitored if it's scheduled directly from a grid scheduler.
        jobQueue.add(toSchedule);
        processQueue();
    }

    public void requestMonitoring(Job jobToMonitor) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        System.out.println("Requesting monitoring for job " + jobToMonitor.getJobId());

        IGridScheduler backUpTarget = selectResourceManagerForBackUp();
        backUpTarget.monitorPrimary(new MonitoringRequest(id, jobToMonitor));
    }

    public IGridScheduler selectResourceManagerForBackUp() throws RemoteException, NotBoundException, MalformedURLException {
        return gridSchedulerRepository.getEntity(0);
    }

    @Override
    public void finish(Node node, WorkRequest toFinish) throws RemoteException, MalformedURLException, NotBoundException {
        IUser user = (IUser) Naming.lookup(toFinish.getUserUrl());
        user.acceptResult(toFinish.getToExecute());
    }

    private synchronized void release(Job toRelease) {
        load -= toRelease.getDuration();
    }

    // should be run after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while (!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            WorkRequest toRun = jobQueue.poll();
            System.out.println("Running job " + toRun.getToExecute().getJobId());

            Task<Void> run = Task.action(() -> allocatedNode.handle(toRun));
            engine.run(run);
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    /**
     * public method to check the connections the handler is monitoring
     */
    public void checkConnections() {
        hHandler.getStatus();
    }

    /**
     * Shutdown method to kill this RM
     */
    public void shutdown() {
        try {
            rmiServer.unRegister(url);
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

    /**
     * This method is called by the Simulation Launcher to kill the node. However it is also needed to kill the process, so we will have to implement
     * a mechanisim to kill all the nodes in a clean way, probably with Threads
     */
    @Override
    public void shutDown() {
        try {
            rmiServer.unRegister(url);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
