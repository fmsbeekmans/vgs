package santiagoAndFerdy.vgs.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Fydio on 3/18/16.
 */
public class RmiServer {
    public RmiServer(int port) {
        try {
            LocateRegistry.createRegistry(port);
            System.out.println("Registery created");
        } catch (RemoteException e) {
            System.out.println("Registry already exsists");
        }
    }

    public boolean register(String url, Remote impl) {
        try {
            Naming.rebind(url, impl);
            System.out.println("Registered object at " + url);

            return true;
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void unRegister(String url) {
        System.out.println("Unregistering: " + url);
        try {
            Naming.unbind(url);
        } catch (RemoteException | MalformedURLException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
