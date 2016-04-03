package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
import santiagoAndFerdy.vgs.gridScheduler.IGridScheduler;
import santiagoAndFerdy.vgs.resourceManager.IResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;
import santiagoAndFerdy.vgs.user.Repositories;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {

    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {
//        int id = 0;
        int id = Integer.parseInt(args[0]);

        RmiServer server = new RmiServer(1099);

        new GridScheduler(
                server,
                id,
                Repositories.RESOURCE_MANAGER_REPOSITORY,
                Repositories.GRID_SCHEDULER_REPOSITORY);
    }
}
