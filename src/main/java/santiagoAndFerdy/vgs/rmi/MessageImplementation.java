package santiagoAndFerdy.vgs.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Fydio on 3/18/16.
 */
public class MessageImplementation extends UnicastRemoteObject implements MessageProtocol {

    public MessageImplementation() throws RemoteException {
    }

    @Override
    public String getMessage() throws RemoteException {
        return "Hello client!";
    }
}
