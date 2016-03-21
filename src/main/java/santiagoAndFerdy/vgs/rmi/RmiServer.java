package santiagoAndFerdy.vgs.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Fydio on 3/18/16.
 */
public class RmiServer {
    public RmiServer(int port) {
        System.out.println("Start server");

        try {
            LocateRegistry.createRegistry(port);
            System.out.println("Registery created");
        } catch (RemoteException e) {
            System.err.println("Registry already exists");
        }
    }

    public void register(String url, UnicastRemoteObject impl) throws MalformedURLException, RemoteException {
        Naming.rebind(url, impl);
        System.out.println("Registered object at" + url);
    }
}
