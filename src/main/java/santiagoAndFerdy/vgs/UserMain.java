package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.User;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

/**
 * Created by Fydio on 3/18/16.
 */
public class UserMain {
    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, URISyntaxException {

        int id = 0;
//        int id = Integer.parseInt(args[0]);

        RmiServer rmiServer = new RmiServer(1099);

        new User(
                rmiServer,
                id,
                IRepository.Repositories.userRepository,
                IRepository.Repositories.resourceManagerRepository).createJobs(0, 10);
    }
}
