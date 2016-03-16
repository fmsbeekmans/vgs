package santiagoAndFerdy.vgs.model;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by Fydio on 3/7/16.
 */
public class RmiServer {
    private int port;
    private RmiServer rmiServer;

    public RmiServer(int port) throws RemoteException {
        this.port = port;

        LocateRegistry.createRegistry(port);
    }

    public int getPort() {
        return port;
    }
}
