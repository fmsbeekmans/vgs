package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repositories;
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
        if(args.length < 4){
            System.err.println("Please insert User ID, RM destination, number of jobs and duration of the job (ms)");
            return;
        }
        // int id = 0;
        int id = Integer.parseInt(args[0]);
        int toRM = Integer.parseInt(args[1]);
        int numJobs = Integer.parseInt(args[2]);
        int jobDuration = Integer.parseInt(args[3]);
        
        RmiServer rmiServer = new RmiServer(1099);

        new User(rmiServer, id, Repositories.userRepository(), Repositories.resourceManagerRepository()).createJobs(toRM, numJobs, jobDuration);
    }
}
