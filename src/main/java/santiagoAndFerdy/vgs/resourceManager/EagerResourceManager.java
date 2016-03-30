package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import santiagoAndFerdy.vgs.discovery.HeartbeatHandler;
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

    private IRepository<IGridSchedulerResourceManagerClient> gridSchedulerRepository;
    private Map<Integer, String> gridSchedulerUrls;
    private IRepository<IGridSchedulerResourceManagerClient> gsRepo;
    private Map<Integer, HeartbeatHandler> gsHeartBeatHandlers;

    private long                          load;
    private ScheduledExecutorService      timerScheduler;
    private Engine                        engine;


    public EagerResourceManager(int id,
                                int nNodes,
                                String url,
                                RmiServer rmiServer,
                                IRepository<IGridSchedulerResourceManagerClient> gridSchedulerRepository) throws RemoteException, MalformedURLException, NotBoundException {
        super();
        this.id = id;
        this.url = url;
        this.nNodes = nNodes;
        this.load = 0;
        // node queues synchronisation need the same mutex anyway. Don't use need to use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(nNodes);

        this.rmiServer = rmiServer;
        this.gridSchedulerRepository = gridSchedulerRepository;
        gridSchedulerUrls = gridSchedulerRepository.urls();
        gsHeartBeatHandlers = new HashedMap<>();
        Map<Integer, String> gsUrls = gridSchedulerRepository.urls();
        for (int gsId : gridSchedulerRepository.ids())  gsHeartBeatHandlers.put(gsId, new HeartbeatHandler(url, gsUrls.get(gsId), rmiServer));


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
        jobQueue.add(req);
        System.out.println("Received job " + req.getJob().getJobId());
        load += req.getJob().getDuration();

        // send to GS first
        Task<Void> backUp = Task.action(() -> requestMonitoring(req.getJob()));
        // send to node after ACK
        Task<Void> process = Task.action(() -> processQueue());
        engine.run(process.andThen(process));
    }

    public void requestMonitoring(Job jobToMonitor) {
        int targetGridSchedulerId = selectResourceManagerForBackUp();
        String targetGridSchedulerUrl = gridSchedulerUrls.get(targetGridSchedulerId);
        MonitorRequest monitorRequest = new MonitorRequest(targetGridSchedulerUrl, jobToMonitor);

        Optional<IGridSchedulerResourceManagerClient> backUpTarget = gridSchedulerRepository.getEntity(selectResourceManagerForBackUp());

        backUpTarget.ifPresent(target -> {
            try {
                target.monitorPrimary(monitorRequest).await();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO: Cancel when down
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        });

    }

    public int selectResourceManagerForBackUp() {
        return 0;
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
