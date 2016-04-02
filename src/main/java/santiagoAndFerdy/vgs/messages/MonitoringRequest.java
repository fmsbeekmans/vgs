package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class MonitoringRequest implements Serializable {
    private @NotNull String sourceClientUrl;
    private @NotNull Job jobToBackUp;

    public MonitoringRequest(String sourceClientUrl, Job jobToBackUp) {
        this.sourceClientUrl = sourceClientUrl;
        this.jobToBackUp = jobToBackUp;
    }

    public String getSourceClientUrl() {
        return sourceClientUrl;
    }

    public Job getJobToMonitor() {
        return jobToBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitoringRequest that = (MonitoringRequest) o;

        if (sourceClientUrl != null ? !sourceClientUrl.equals(that.sourceClientUrl) : that.sourceClientUrl != null)
            return false;
        return jobToBackUp != null ? jobToBackUp.equals(that.jobToBackUp) : that.jobToBackUp == null;

    }

    @Override
    public int hashCode() {
        int result = sourceClientUrl != null ? sourceClientUrl.hashCode() : 0;
        result = 31 * result + (jobToBackUp != null ? jobToBackUp.hashCode() : 0);
        return result;
    }
}
