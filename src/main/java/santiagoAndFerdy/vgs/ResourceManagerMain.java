package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.rmi.MessageImplementation;
import santiagoAndFerdy.vgs.rmi.RMIServer;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    public static void main(String[] args) throws RemoteException, MalformedURLException {
        RMIServer server = new RMIServer();
        server.register("test", new MessageImplementation());
        while (true) {}
    }
}
