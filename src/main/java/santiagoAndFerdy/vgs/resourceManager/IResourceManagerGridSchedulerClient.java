package santiagoAndFerdy.vgs.resourceManager;

import com.linkedin.parseq.promise.Promise;
import santiagoAndFerdy.vgs.discovery.IAddressable;
import santiagoAndFerdy.vgs.messages.UserRequest;

import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/30/16.
 */
public interface IResourceManagerGridSchedulerClient extends IAddressable {

    /**
     * Schedule a job on this cluster managed by this resource manager.
     * Must be accepted
     * @param userRequest to schedule
     * @return a promise that will be fulliflled to signify ACKnowledgement
     * @throws RemoteException
     */
    Promise<Void> schedule(UserRequest userRequest) throws RemoteException;
}
