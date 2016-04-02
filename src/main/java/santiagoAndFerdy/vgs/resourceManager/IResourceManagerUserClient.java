package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.discovery.IAddressable;
import santiagoAndFerdy.vgs.model.Job;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/20/16.
 */
public interface IResourceManagerUserClient extends IAddressable {
    // for the driver
    void acceptResult(Job j) throws RemoteException;
    String getUrl() throws RemoteException;

    // for the user
    Promise<Void> schedule(Job j) throws MalformedURLException, RemoteException, NotBoundException;
}
