package santiagoAndFerdy.vgs.cluster;

import com.linkedin.parseq.Task;
import santiagoAndFerdy.vgs.model.Job;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/20/16.
 */
public interface IResourceManagerProxy extends Remote {
    // for the driver
    void acceptResult(Job j) throws RemoteException;
    String getUrl() throws RemoteException;

    // for the user
    Task<Void> schedule(Job j) throws MalformedURLException, RemoteException, NotBoundException;
}
