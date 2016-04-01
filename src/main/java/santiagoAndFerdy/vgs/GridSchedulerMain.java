package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;

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
import santiagoAndFerdy.vgs.gridScheduler.GridScheduler;
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
        String url = args[0];
        String bucketName = args[1];
        String fileName = args[2];
        
        credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        
        s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, fileName));
                
        IRepository<IHeartbeatReceiver> repoRM = Repository.fromS3(s3object.getObjectContent());
        RmiServer server = new RmiServer(1099);

        GridScheduler gs = new GridScheduler(server, repoRM, url);
        server.register(url, gs);
        while (true) {
            Thread.sleep(2000);
            gs.checkConnections();
            System.out.println("");
        }

    }
}
