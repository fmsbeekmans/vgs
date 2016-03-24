package santiagoAndFerdy.vgs.discovery;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Fydio on 3/24/16.
 */
class RemoteMock extends UnicastRemoteObject implements IRemoteMock {
    private double value;

    public RemoteMock(double value) throws RemoteException {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }
}
