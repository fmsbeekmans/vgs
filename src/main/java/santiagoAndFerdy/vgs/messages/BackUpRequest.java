package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerGridSchedulerClient;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerResourceManagerClient;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {
    private String sourceUrl;
    private @NotNull Job jobToBackUp;

    public BackUpRequest(String sourceUrl, Job jobToBackUp) {
        this.sourceUrl = sourceUrl;
        this.jobToBackUp = jobToBackUp;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Job getJobToBackUp() {
        return jobToBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        if (sourceUrl != null ? !sourceUrl.equals(that.sourceUrl) : that.sourceUrl != null) return false;
        return jobToBackUp != null ? jobToBackUp.equals(that.jobToBackUp) : that.jobToBackUp == null;

    }

    @Override
    public int hashCode() {
        int result = sourceUrl != null ? sourceUrl.hashCode() : 0;
        result = 31 * result + (jobToBackUp != null ? jobToBackUp.hashCode() : 0);
        return result;
    }
}
