package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.model.user.User;
import santiagoAndFerdy.vgs.rmi.MessageProtocol;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException, InterruptedException {
        System.out.println("I'm a user");
        RmiServer rmiServer = new RmiServer(1089);

        User u = new User(rmiServer, "localhost/proxy", "localhost/rm");
        u.start();

        Thread.sleep(30000);
    }
}
