package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.model.cluster.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws RemoteException, MalformedURLException {
        RmiServer server = new RmiServer(1099);

        EagerResourceManager rmImpl = new EagerResourceManager(10000);
        server.register("localhost/rm", rmImpl);

        while (true) {}
    }
}
