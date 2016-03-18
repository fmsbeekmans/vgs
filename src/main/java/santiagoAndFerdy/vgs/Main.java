package santiagoAndFerdy.vgs;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.model.cluster.ResourceManager;
import santiagoAndFerdy.vgs.model.messages.IMessageReceivedHandler;
import santiagoAndFerdy.vgs.model.messages.Message;
import santiagoAndFerdy.vgs.rmi.RMIServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class Main extends UnicastRemoteObject implements Runnable, IMessageReceivedHandler {
	public Main() throws RemoteException {
		i();
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException {
		Main m = new Main();

	}

	public static void sendMessage(Message m, String url) {
		try {
			IMessageReceivedHandler stub = (IMessageReceivedHandler) java.rmi.Naming.lookup(url);
			stub.onMessageReceived(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void i() throws RemoteException {
		// Init server
		RMIServer server = new RMIServer();
		// Create RM
		ResourceManager rm = new ResourceManager("0", "localhost");
		Thread t = new Thread(this);
		t.run();
		System.out.println("We exit");

	}

	@Override
	public void run() {
		// To avoid rejection on RMI invocations
		System.setProperty("java.security.policy", "file:./my.policy");
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		// Let's bind ourselves to the server
		try {
			try {
				String url = "rmi://" + "localhost" + ":1099/customer";
				Naming.rebind(url, this);
				System.out.println("Customer" + " binded to: " + url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String template = "rmi://localhost:1099/RM_";
		// Let's create a Job of 10s with ID 1 and id of RM 0
		Job job = new Job(10000, 1, 0);
		// Message to send the Job
		String url = template + "0";
		Message m = new Message("JOB_REQUEST", job, url);
		sendMessage(m, url);
	}

	@Override
	public void onMessageReceived(Message message) throws RemoteException {
		if (message != null) {
			if (message.getType().equals("JOB_ACK")) {
				System.out.println("ACK RECEIVED!!");
			}
		}

	}
}
