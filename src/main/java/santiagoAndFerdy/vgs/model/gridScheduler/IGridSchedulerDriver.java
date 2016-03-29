package santiagoAndFerdy.vgs.model.gridScheduler;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.messages.Heartbeat;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/19/16.
 */
public interface IGridSchedulerDriver extends Remote {
    void ping(@NotNull Heartbeat h) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;


}
