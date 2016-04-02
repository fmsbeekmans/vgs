package santiagoAndFerdy.vgs.discovery;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAdressable extends Remote {
    int getId() throws RemoteException;
    String getUrl() throws RemoteException;
}
