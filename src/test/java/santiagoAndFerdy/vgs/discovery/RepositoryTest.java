package santiagoAndFerdy.vgs.discovery;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.BeforeClass;
import org.junit.Test;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Fydio on 3/24/16.
 */
public class RepositoryTest {
    private static RmiServer rmiServer;

    @Test
    public void emptyIdListingTest() {
        IRepository<IRemoteMock> repo = getRepository(new HashMap<>());

        assertTrue(repo.ids().isEmpty());
    }
    @Test
    public void idListingTest() {
        Map<Integer, String> entries = new HashMap<>();
        entries.put(3, "entry");
        entries.put(8, "entry");
        IRepository<IRemoteMock> repo = getRepository(entries);

        assertTrue(repo.ids().contains(3));
        assertTrue(repo.ids().contains(8));
    }

    @Test
    public void getRemoteTest() throws IOException {
        List<RepositoryEntry<IRemoteMock>> remoteEntries = new ArrayList<>();
        remoteEntries.add(new RepositoryEntry<>(0, "zero", new RemoteMock(0d)));

        String numberFileName = "numbersListing";
        Path numberFilePath = setupRmiAndFs(rmiServer, remoteEntries, numberFileName);

    }

    public IRepository<IRemoteMock> getRepository(Map<Integer, String> entries) {
        return new Repository<IRemoteMock>(entries);
    }

    @BeforeClass
    public static void createRegistery() {
        rmiServer = new RmiServer(1099);
    }

    public static Path setupRmiAndFs(RmiServer rmiServer, Iterable<RepositoryEntry<IRemoteMock>> entities, String fileName) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

        Path path = fs.getPath(fileName);
        List<String> lines = new ArrayList<>(2);

        for (RepositoryEntry<IRemoteMock> e : entities) {
            lines.add(e.getId() + " " + e.getUrl());
            rmiServer.register(e.getUrl(), e.getEntity());
        }

        Files.write(path, lines);

        return path;
    }
}
