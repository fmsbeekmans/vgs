package santiagoAndFerdy.vgs.discovery;

import java.rmi.RemoteException;

/**
 * Created by Fydio on 4/4/16.
 */
public interface ICrashable extends IAddressable {
    void shutDown() throws RemoteException;
    void start() throws RemoteException;
}
