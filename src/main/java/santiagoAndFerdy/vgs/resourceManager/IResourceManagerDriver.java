package santiagoAndFerdy.vgs.resourceManager;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.discovery.IAddressable;
import santiagoAndFerdy.vgs.messages.UserRequest;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IResourceManagerDriver extends IAddressable {
    void queue(@NotNull UserRequest req) throws RemoteException, MalformedURLException, NotBoundException;
    void respond(@NotNull UserRequest req) throws RemoteException, MalformedURLException, NotBoundException;
    void finish(Node node, @NotNull UserRequest req) throws RemoteException, NotBoundException, MalformedURLException;
    @NotNull
    ScheduledExecutorService executorService() throws RemoteException;
}
