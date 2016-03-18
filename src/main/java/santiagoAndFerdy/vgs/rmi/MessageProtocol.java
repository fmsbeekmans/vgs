package santiagoAndFerdy.vgs.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public interface MessageProtocol extends Remote {
    public String getMessage() throws RemoteException;
}
