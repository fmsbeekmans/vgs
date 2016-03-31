package santiagoAndFerdy.vgs.discovery;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Entity extends Remote {
    int getId() throws RemoteException;
    String getUrl() throws RemoteException;
}
