package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAddressable extends Remote {
    int getId() throws RemoteException;

    /**
     * See if this adressable unit is still alive, will ACK with it's load.
     * @return load
     * @throws MalformedURLException
     * @throws RemoteException
     * @throws NotBoundException
     */
    long ping() throws RemoteException;
}
