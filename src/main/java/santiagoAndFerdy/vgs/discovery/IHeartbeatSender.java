package santiagoAndFerdy.vgs.discovery;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.messages.Heartbeat;

public interface IHeartbeatSender extends Remote {
    void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException;
}
