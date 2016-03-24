package santiagoAndFerdy.vgs.discovery;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/24/16.
 */
interface IRemoteMock extends Remote {
    double getValue() throws RemoteException;
}
