package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class MonitoringRequest implements Serializable {
    private @NotNull int sourceResourceManagerId;
    private @NotNull Job jobToMonitor;

    public MonitoringRequest(int sourceResourceManagerId, Job jobToBackUp) {
        this.sourceResourceManagerId = sourceResourceManagerId;
        this.jobToMonitor = jobToBackUp;
    }

    public int getSourceResourceManagerId() {
        return sourceResourceManagerId;
    }

    public Job getJobToMonitor() {
        return jobToMonitor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitoringRequest that = (MonitoringRequest) o;

        if (sourceResourceManagerId != that.sourceResourceManagerId) return false;
        return jobToMonitor != null ? jobToMonitor.equals(that.jobToMonitor) : that.jobToMonitor == null;

    }

    @Override
    public int hashCode() {
        int result = sourceResourceManagerId;
        result = 31 * result + (jobToMonitor != null ? jobToMonitor.hashCode() : 0);
        return result;
    }
}
