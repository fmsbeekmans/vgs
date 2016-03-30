package santiagoAndFerdy.vgs.gridScheduler;

import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import santiagoAndFerdy.vgs.messages.MonitorRequest;
import santiagoAndFerdy.vgs.messages.UserRequest;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GridSchedulerResourceManagerClient extends UnicastRemoteObject implements IGridSchedulerResourceManagerClient {

    private RmiServer rmiServer;
    private int id;
    private String url;
    private String driverUrl;

    private Map<Job, SettablePromise<Void>> pendingBackupRequests;

    public GridSchedulerResourceManagerClient(RmiServer rmiServer, int id, String url, String driverUrl) throws RemoteException {
        this.rmiServer = rmiServer;
        this.id = id;
        this.url = url;
        this.driverUrl = driverUrl;

        this.pendingBackupRequests = new HashMap<>();
    }

    @Override
    public Promise<Void> monitorPrimary(Job jobToMonitor) throws RemoteException, MalformedURLException, NotBoundException {
        MonitorRequest monitorRequest = new MonitorRequest(url, jobToMonitor);
        SettablePromise<Void> monitorPromise = Promises.settable();
        pendingBackupRequests.put(monitorRequest.getJobToMonitor(), monitorPromise);

        IGridSchedulerDriver driver = (IGridSchedulerDriver) Naming.lookup(driverUrl);
        driver.monitorPrimary(monitorRequest);

        return monitorPromise;
    }

    @Override
    public void monitoringRequestAccepted(Job job) {
        Optional<SettablePromise<Void>> monitorPromise = Optional.ofNullable(pendingBackupRequests.get(job));
        monitorPromise.ifPresent(p -> p.done(null));
    }

    @Override
    public Promise<Void> offload(UserRequest userRequest) {
        return null;
    }

    @Override
    public Promise<Void> releaseResources(int requestId) {
        return null;
    }
}
