package santiagoAndFerdy.vgs.user;

import santiagoAndFerdy.vgs.discovery.IAddressable;
import santiagoAndFerdy.vgs.model.Job;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 4/3/16.
 */
public interface IUser extends Remote {
    void acceptResult(Job j) throws RemoteException;
    void createJobs(int rmId, int ms, int n) throws RemoteException;
}
