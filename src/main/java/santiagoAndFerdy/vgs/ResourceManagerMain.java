package santiagoAndFerdy.vgs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import santiagoAndFerdy.vgs.discovery.IRepository;
import santiagoAndFerdy.vgs.discovery.Repository;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerGridSchedulerClient;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.resourceManager.EagerResourceManager;
import santiagoAndFerdy.vgs.rmi.RmiServer;

/**
 * Created by Fydio on 3/18/16.
 */
public class ResourceManagerMain {
    private static AWSCredentials credentials;
    private static AmazonS3       s3Client;
    
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        if (args.length < 4) {
            System.err.println("Please enter the URL of this ResourceManager, the id, the bucket name and the GS file name");
            return;
        }

        // Probably will have to pass the number of nodes as a parameter as well...
        String url = args[0];
        int id = Integer.valueOf(args[1]);
        String bucketName = args[2];
        String fileName = args[3];

        credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(e.getMessage());
        }
        
        s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
        S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, fileName));
        IRepository<IGridSchedulerResourceManagerClient> repoGS = Repository.fromS3(s3object.getObjectContent());

        RmiServer server = new RmiServer(1099);
        EagerResourceManager rmImpl = new EagerResourceManager(id, 10000, server, url, repoGS);
        server.register(url, rmImpl);

    }
}
