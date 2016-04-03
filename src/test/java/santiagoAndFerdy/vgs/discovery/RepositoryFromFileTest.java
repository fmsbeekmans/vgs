package santiagoAndFerdy.vgs.discovery;

import org.junit.Test;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by Fydio on 3/24/16.
 */
public class RepositoryFromFileTest extends RepositoryTest {

    @Test
    public void repositoryFromFileTest() throws IOException {
        RmiServer rmiServer = new RmiServer(1099);
        List<RepositoryEntry<IRemoteMock>> remoteEntries = new ArrayList<>();
        RemoteMock testEntity = new RemoteMock(0d);
        remoteEntries.add(new RepositoryEntry<>(0, "zero", testEntity));

        String numberFileName = "numbersListing";
        Path numberFilePath = setupRmiAndFs(rmiServer, remoteEntries, numberFileName);

        IRepository<IRemoteMock> numberRepo = Repository.fromFile(numberFilePath);

        assertEquals("Should be able to get registered object", Optional.of(0d), numberRepo.getEntity(0).flatMap(e -> {
            try {
                return Optional.of(e.getValue());
            } catch (RemoteException e1) {
                e1.printStackTrace();
                return Optional.empty();
            }
        }));

        boolean result = numberRepo.setLastKnownStatus(0, Status.ONLINE);
    }


}
