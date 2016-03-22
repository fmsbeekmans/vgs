package santiagoAndFerdy.vgs.model.cluster;

import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import santiagoAndFerdy.vgs.model.Request;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Fydio on 3/19/16.
 */
public class EagerResourceManager extends UnicastRemoteObject implements IResourceManagerDriver {
    private Queue<Request> jobQueue;
    private Queue<Node> idleNodes;
    private int n;

    private ScheduledExecutorService executorService;

    public EagerResourceManager(int n) throws RemoteException {
        super();

        this.n = n;

        // node queues synchronisation need the same mutex anyway. Don't use threadsafe queue
        jobQueue = new LinkedBlockingQueue<>();
        idleNodes = new CircularFifoQueue<>(n);

        for(int i = 0; i < n; i++) idleNodes.add(new Node(i, this));

        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public synchronized void queue(Request req) throws RemoteException, MalformedURLException, NotBoundException {
        jobQueue.add(req);
        System.out.println("Received job " + req.getJob().getJobId());
        processQueue();
    }

    @Override
    public void respond(Request req) throws RemoteException, MalformedURLException, NotBoundException {
        IResourceManagerProxy client = (IResourceManagerProxy) Naming.lookup(req.getUser().getUrl());
        client.acceptResult(req.getJob());
    }

    @Override
    public synchronized void finish(Node node, Request req) throws RemoteException, NotBoundException, MalformedURLException {
        respond(req);
        idleNodes.add(node);
        processQueue();
    }

    // should be ran after any change in node state or job queue
    protected synchronized void processQueue() throws RemoteException, MalformedURLException, NotBoundException {
        while(!jobQueue.isEmpty() && !idleNodes.isEmpty()) {
            Node allocatedNode = idleNodes.poll();
            Request req = jobQueue.poll();
            System.out.println("Running job " + req.getJob().getJobId());
            allocatedNode.handle(req);
        }
    }

    @Override
    @NotNull
    public ScheduledExecutorService executorService() throws RemoteException {
        return executorService;
    }
}
