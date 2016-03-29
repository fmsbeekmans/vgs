package santiagoAndFerdy.vgs.cluster;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Request;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IResourceManagerDriver extends Remote {
    void queue(@NotNull Request req) throws RemoteException, MalformedURLException, NotBoundException;
    void respond(@NotNull Request req) throws RemoteException, MalformedURLException, NotBoundException;
    void finish(Node node, @NotNull Request req) throws RemoteException, NotBoundException, MalformedURLException;
    @NotNull
    ScheduledExecutorService executorService() throws RemoteException;
}
