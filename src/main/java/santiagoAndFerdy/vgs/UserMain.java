package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerDriver;
import santiagoAndFerdy.vgs.model.cluster.IResourceManagerUserClient;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.model.user.User;
import santiagoAndFerdy.vgs.rmi.MessageProtocol;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {
        System.out.println("I'm a user");
        RmiServer rmiServer = new RmiServer(1089);
        URL url = UserMain.class.getClassLoader().getResource("user/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManagerDriver> repo = Repository.fromFile(rmRepositoryFilePath);

        User u = new User(rmiServer, "localhost/proxy", repo);

        System.out.println("I'm done.");
    }
}
