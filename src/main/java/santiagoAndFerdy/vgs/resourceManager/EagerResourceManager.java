package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
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
import santiagoAndFerdy.vgs.messages.BackUpRequest;
import santiagoAndFerdy.vgs.messages.Heartbeat;
import santiagoAndFerdy.vgs.messages.IRemoteShutdown;
import santiagoAndFerdy.vgs.user.IUser;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManager, IRemoteShutdown, Runnable {
    private static final long           serialVersionUID = -4089353922882117112L;

    private Queue<WorkRequest>          jobQueue;
    private Queue<Node>                 idleNodes;
    private Queue<Task<Void>>           taskQueue;
    private RmiServer                   rmiServer;
    private int                         nNodes;
    private int                         id;
    private String                      url;
    private IGridScheduler              mainGS;
    private IGridScheduler              backUpGS;
    private IRepository<IGridScheduler> gridSchedulerRepository;
    private HeartbeatHandler            hHandler;
    private long                        load;
    private ScheduledExecutorService    timerScheduler;
    private Engine                      engine;

    private boolean                     running;
    private Thread                      pollThread;
    private final int                   pollingInterval  = 200;

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
    public EagerResourceManager(int id, int n, RmiServer rmiServer, String url, IRepository<IGridScheduler> gridSchedulerRepository)
            throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.id = id;
        this.url = url;
        this.nNodes = n;
        this.load = 0;
        taskQueue = new CircularFifoQueue<>();
        // node queues synchronisation need the same mutex anyway. Don't use need to use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(nNodes);

        this.rmiServer = rmiServer;
        this.gridSchedulerRepository = gridSchedulerRepository;

        hHandler = new HeartbeatHandler(gridSchedulerRepository, id, false);
        for (int i = 0; i < nNodes; i++)
            idleNodes.add(new Node(i, this, timerScheduler));

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();
        pollThread = new Thread(this, "RM"+id);
        running = true;
        pollThread.start();

    }

    @Override
    public synchronized void offerWork(WorkRequest req) throws RemoteException, MalformedURLException, NotBoundException {
        if (mainGS == null)
            mainGS = selectMainGS();
        if (backUpGS == null)
            backUpGS = selectBackUpGS();
        System.out.println("Received job " + req.getJob().getJobId() + " at cluster " + mainGS.getId());

        Task<Void> queueFlow = Task
                // send to GS first
                .action(() -> requestMonitoring(req))
                // then schedule
                .andThen(monitored -> jobQueue.add(req))
                // and process the queue again
                .andThen(queue -> processQueue());
        taskQueue.add(queueFlow);
        load += req.getJob().getDuration();
    }

    @Override
    public void schedule(@NotNull WorkRequest toSchedule) throws RemoteException, MalformedURLException, NotBoundException {
        // Assume a job is already monitored if it's scheduled directly from a grid scheduler.
        jobQueue.add(toSchedule);
        processQueue();
    }

    public void requestMonitoring(WorkRequest jobToMonitor) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        //System.out.println("Requesting monitoring for job " + jobToMonitor.getJob().getJobId());
        mainGS.monitorPrimary(new MonitoringRequest(id, jobToMonitor));
        backUpGS.monitorBackUp(new BackUpRequest(id, jobToMonitor));

    }

    /**
     * 
     * @return First GS on the repository which is the main one
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    public IGridScheduler selectMainGS() throws RemoteException, NotBoundException, MalformedURLException {
        return gridSchedulerRepository.getEntity(gridSchedulerRepository.ids().get(0));
    }

    /**
     * 
     * @return Second GS on the repository (the backup)
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    public IGridScheduler selectBackUpGS() throws RemoteException, NotBoundException, MalformedURLException {
        return gridSchedulerRepository.getEntity(gridSchedulerRepository.ids().get(1));
    }

    @Override
    public void finish(Node node, WorkRequest toFinish) throws RemoteException, MalformedURLException, NotBoundException {
        release(toFinish);
        mainGS.releaseResources(new MonitoringRequest(id, toFinish)); // this shouldn't be done like this...
        backUpGS.releaseBackUp(new BackUpRequest(id, toFinish));
        node.setIdle();
        idleNodes.offer(node);
        IUser user = (IUser) Naming.lookup(toFinish.getUserUrl());
        user.acceptResult(toFinish.getJob());
    }

    private synchronized void release(WorkRequest toRelease) {
        load -= toRelease.getJob().getDuration();
    }

    // should be run after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while (!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            WorkRequest toRun = jobQueue.poll();
            System.out.println("Running job " + toRun.getJob().getJobId());

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
            running = false;
            pollThread.join();
            rmiServer.unRegister(url);
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void processTasks(){
        while(!taskQueue.isEmpty())
        engine.run(taskQueue.poll());
    }
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(pollingInterval);
                processTasks();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
