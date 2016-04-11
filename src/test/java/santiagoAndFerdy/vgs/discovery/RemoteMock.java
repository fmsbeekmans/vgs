package santiagoAndFerdy.vgs.discovery;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Fydio on 3/24/16.
 */
class RemoteMock extends UnicastRemoteObject implements IRemoteMock {
    private double value;
    private int id;
    private long load;


    public RemoteMock(double value) throws RemoteException {
        this.value = value;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    public void setLoad(long load) {
        this.load = load;
    }

    @Override
    public long ping() throws RemoteException {
        return load;
    }
}
