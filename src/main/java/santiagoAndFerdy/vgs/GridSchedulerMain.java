package santiagoAndFerdy.vgs;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.model.cluster.GridScheduler;
import santiagoAndFerdy.vgs.model.messages.Heartbeat;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {
	public static void main(String[] args) throws RemoteException, MalformedURLException, InterruptedException, NotBoundException {
		System.out.println("I'm a grid scheduler");
		RmiServer server = new RmiServer(1099);
		GridScheduler gs = new GridScheduler(server);
		server.register("localhost/gs", gs);
		Thread.sleep(2000);
		while (true) {

		}
	}
}
