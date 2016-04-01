package santiagoAndFerdy.vgs.messages;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemoteShutdown extends Remote {
void shutDown() throws RemoteException;
}
