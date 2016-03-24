package santiagoAndFerdy.vgs.model.cluster;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.EngineBuilder;
import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import santiagoAndFerdy.vgs.model.Request;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManagerDriver {
    private Queue<Request> jobQueue;
    private Queue<Node> idleNodes;
    private int id;
    private int n;
    private long load;

    private ScheduledExecutorService timerScheduler;
    private Engine engine;

    public EagerResourceManager(int id, int n) throws RemoteException {
        super();

        this.id = id;
        this.n = n;
        this.load = 0;

        // node queues synchronisation need the same mutex anyway. Don't use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(n);

        for(int i = 0; i < n; i++) idleNodes.add(new Node(i, this));

        // setup async machinery
        timerScheduler = Executors.newSingleThreadScheduledExecutor();
        int numCores = Runtime.getRuntime().availableProcessors();
        ExecutorService taskScheduler = Executors.newFixedThreadPool(numCores + 1);

        engine = new EngineBuilder()
                .setTaskExecutor(taskScheduler)
                .setTimerScheduler(timerScheduler)
                .build();
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

    // should be ran after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while(!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            Request req = jobQueue.poll();
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
