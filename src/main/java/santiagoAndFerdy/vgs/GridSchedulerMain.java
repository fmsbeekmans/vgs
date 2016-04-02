package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.GridSchedulerDriver;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerGridSchedulerClient;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;
import com.amazonaws.AmazonClientException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;

import santiagoAndFerdy.vgs.discovery.IHeartbeatReceiver;
import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class GridSchedulerMain {
    private static AWSCredentials credentials;
    private static AmazonS3       s3Client;

    public static void main(String[] args) throws InterruptedException, NotBoundException, URISyntaxException, IOException {
        if (args.length < 3) {
            System.err.println("Please enter the URL of this GridScheduler, the bucket name and the RM file name");
            return;
        }

        int id = Integer.parseInt(args[0]);
        String url = args[1];
        String bucketName = args[2];
        String resourceManagerListingFileName = args[3];
        String gridSchedulerListingFileName = args[4];

        credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }

        s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object resourceManagerListing = s3Client.getObject(new GetObjectRequest(bucketName, resourceManagerListingFileName));
        S3Object gridSchedulerListing = s3Client.getObject(new GetObjectRequest(bucketName, resourceManagerListingFileName));

        IRepository<IResourceManagerGridSchedulerClient> resourceManagerClientRepository = Repository.fromS3(gridSchedulerListing.getObjectContent());
        IRepository<IGridSchedulerGridSchedulerClient> gridSchedulerClientRepository = Repository.fromS3(gridSchedulerListing.getObjectContent());
        RmiServer server = new RmiServer(1099);

        GridSchedulerDriver gs = new GridSchedulerDriver(
                server,
                resourceManagerClientRepository,
                gridSchedulerClientRepository,
                url,
                id);
        server.register(url, gs);

        //TODO check connections?
    }
}
