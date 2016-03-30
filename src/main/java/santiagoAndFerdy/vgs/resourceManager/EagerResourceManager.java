package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.messages.MonitorRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManagerDriver {
    private Queue<UserRequest>            jobQueue;
    private Queue<Node>                   idleNodes;

    private RmiServer                     rmiServer;
    private int nNodes;
    private int                           id;
    private String url;

    private Map<Integer, IGridSchedulerResourceManagerClient> gridSchedulerClients;

    private long                          load;
    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;


    public EagerResourceManager(int id,
                                int nNodes,
                                String url,
                                RmiServer rmiServer,
                                Map<Integer, IGridSchedulerResourceManagerClient> gridSchedulerClients)
            throws RemoteException, MalformedURLException, NotBoundException {

        super();
        this.id = id;
        this.url = url;
        this.nNodes = nNodes;
        this.load = 0;
        // node queues synchronisation need the same mutex anyway. Don't use need to use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(nNodes);

        this.rmiServer = rmiServer;
        this.gridSchedulerClients = gridSchedulerClients;

        for (int i = 0; i < nNodes; i++)
            idleNodes.add(new Node(i, this));

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);
        engine = new EngineBuilder().setTaskExecutor(taskScheduler).setTimerScheduler(timerScheduler).build();

    }

    @Override
    public synchronized void queue(UserRequest req) throws RemoteException, MalformedURLException, NotBoundException {
        System.out.println("Received job " + req.getJob().getJobId());
        load += req.getJob().getDuration();

        Task<Void> queueFlow = Task
                // send to GS first
                .action(() -> requestMonitoring(req.getJob()))
                // then schedule
                .andThen(monitored -> jobQueue.add(req))
                // and process the queue again
                .andThen(queue -> processQueue());
        engine.run(queueFlow);
    }

    public void requestMonitoring(Job jobToMonitor) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        System.out.println("Requesting monitoring for job " + jobToMonitor.getJobId());

        IGridSchedulerResourceManagerClient backUpTarget = selectResourceManagerForBackUp();

        Promise<Void> monitorPromise = backUpTarget.monitorPrimary(jobToMonitor);
        monitorPromise.await();
        System.out.println("Job " + jobToMonitor.getJobId() + " is being monitored");
    }

    public IGridSchedulerResourceManagerClient selectResourceManagerForBackUp() {
        return gridSchedulerClients.get(0);
    }

    @Override
    public void respond(UserRequest req) throws RemoteException, MalformedURLException, NotBoundException {
        Task<Void> respond = Task.action(() -> {
            IResourceManagerUserClient client = (IResourceManagerUserClient) Naming.lookup(req.getUser().getUrl());
            client.acceptResult(req.getJob());
            release(req);
        });

        engine.run(respond);
    }

    private synchronized void release(UserRequest req) {
        load -= req.getJob().getDuration();
    }

//    public void startHBHandler() {
//        try {
//            hHandler = new HeartbeatHandler("localhost/hRM", "localhost/hGS", rmiServer);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        } catch (NotBoundException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public synchronized void finish(Node node, UserRequest req) throws RemoteException, NotBoundException, MalformedURLException {
        respond(req);
        idleNodes.add(node);

        Task<Void> process = Task.action(() -> processQueue());
        engine.run(process);
    }

    // should be ran after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while (!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            UserRequest req = jobQueue.poll();
            System.out.println("Running job " + req.getJob().getJobId());

            Task<Void> run = Task.action(() -> allocatedNode.handle(req));
            engine.run(run);
        }
    }

    @Override
    @NotNull
    public ScheduledExecutorService executorService() throws RemoteException {
        return timerScheduler;
    }
}
