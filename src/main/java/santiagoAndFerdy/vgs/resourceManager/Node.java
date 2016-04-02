package santiagoAndFerdy.vgs.resourceManager;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.messages.UserRequest;

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
    private UserRequest userRequest;
    private IResourceManager rm;


    public Node(int id, @NotNull IResourceManager rm) {
        this.id = id;
        this.rm = rm;
    }

    public int getId() {
        return id;
    }

    public Optional<UserRequest> getUserRequest() {
        return Optional.ofNullable(userRequest);
    }

    public synchronized void handle(@NotNull UserRequest userRequest) throws RemoteException, MalformedURLException, NotBoundException {
        this.userRequest = userRequest;

        this.rm.executorService()
                .schedule(() -> {
                    try {
                        this.rm.finish(this, userRequest);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    }
                },
                userRequest.getJob().getDuration(), TimeUnit.MILLISECONDS);
    }

    public void setIdle() throws RemoteException, MalformedURLException, NotBoundException {
        this.userRequest = null;
    }

    public IResourceManager getRm() {
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
