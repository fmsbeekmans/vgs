package santiagoAndFerdy.vgs.discovery;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;
import santiagoAndFerdy.vgs.rmi.RmiServer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by Fydio on 3/24/16.
 */
public class RepositoryTest {
    class RepositoryEntry<T extends Remote> {
        private int id;
        private String url;
        private T entity;

        public RepositoryEntry(int id, String url, T entity) {
            this.id = id;
            this.url = url;
            this.entity = entity;
        }

        public int getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public T getEntity() {
            return entity;
        }
    }

    interface IRemoteMock extends Remote {
        double getValue() throws RemoteException;
    }

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

    public static <T extends Remote> Path setupRmiAndFs(RmiServer rmiServer, Iterable<RepositoryEntry<T>> entities, String fileName) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());

        Path path = fs.getPath(fileName);
        List<String> lines = new ArrayList<String>(2);

        for (RepositoryEntry<T> e : entities) {
            lines.add(e.getId() + " " + e.getUrl());
            rmiServer.register(e.getUrl(), e.entity);
        }

        Files.write(path, lines);

        return path;
    }
}
