package santiagoAndFerdy.vgs.model;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerGridSchedulerClient;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {
    private @NotNull IResourceManagerGridSchedulerClient source;
    private @NotNull Job jobToBackUp;

    public BackUpRequest(IResourceManagerGridSchedulerClient source, Job j) {
        this.source = source;
        this.jobToBackUp = j;
    }

    public IResourceManagerGridSchedulerClient getSource() {
        return source;
    }

    public Job getJobToBackUp() {
        return jobToBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        return jobToBackUp != null ? jobToBackUp.equals(that.jobToBackUp) : that.jobToBackUp == null;

    }

    @Override
    public int hashCode() {
        return jobToBackUp != null ? jobToBackUp.hashCode() : 0;
    }
}
