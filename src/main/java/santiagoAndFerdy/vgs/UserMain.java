package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    final static String urlUser = "//localhost/user";
    
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {
        
        RmiServer rmiServer = new RmiServer(1099);
        URL url = UserMain.class.getClassLoader().getResource("user/rms");
        Path rmRepositoryFilePath = Paths.get(url.toURI());
        IRepository<IResourceManager> resourceManagerRepository = Repository.fromFile(rmRepositoryFilePath);
        User u = new User(resourceManagerRepository, urlUser, rmiServer);
        rmiServer.register(urlUser, u);
        u.createJobs(0,1);
       // u.createJobs(2,2);
       // u.createJobs(5,2);
    }
}
