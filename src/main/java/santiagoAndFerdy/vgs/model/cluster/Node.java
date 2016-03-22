package santiagoAndFerdy.vgs.model.cluster;

import com.linkedin.parseq.Task;
import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Request;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by Fydio on 3/19/16.
 */
public class Node {
    private int id;
    private Request request;
    private IResourceManagerDriver rm;


    public Node(int id, @NotNull IResourceManagerDriver rm) {
        this.id = id;
        this.rm = rm;
    }

    public int getId() {
        return id;
    }

    public Optional<Request> getRequest() {
        return Optional.ofNullable(request);
    }

    public synchronized void handle(@NotNull Request request) throws RemoteException, MalformedURLException, NotBoundException {
        this.request = request;

        this.rm.executorService()
                .schedule(() -> {
                    try {
                        this.rm.finish(this, request);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    }
                },
                request.getJob().getDuration(), TimeUnit.MILLISECONDS);
    }

    public void setIdle() throws RemoteException, MalformedURLException, NotBoundException {
        this.request = null;
    }

    public IResourceManagerDriver getRm() {
        return rm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        if (id != node.id) return false;
        return rm.equals(node.rm);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + rm.hashCode();
        return result;
    }
}
