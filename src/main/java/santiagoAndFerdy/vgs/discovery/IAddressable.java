package santiagoAndFerdy.vgs.discovery;

import santiagoAndFerdy.vgs.messages.Heartbeat;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAddressable extends Remote {
    int getId() throws RemoteException;

    void iAmAlive(Heartbeat h) throws MalformedURLException, RemoteException, NotBoundException;
}
