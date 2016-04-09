package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class MonitorRequest implements Serializable {

    private static final long serialVersionUID = 1531240896670545486L;
    private @NotNull int sourceResourceManagerId;
    private @NotNull WorkRequest toMonitor;

    public MonitorRequest(int sourceResourceManagerId, WorkRequest jobToMonitor) {
        this.sourceResourceManagerId = sourceResourceManagerId;
        this.toMonitor = jobToMonitor;
    }

    public int getSourceResourceManagerId() {
        return sourceResourceManagerId;
    }

    public WorkRequest getWorkRequest() {
        return toMonitor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitorRequest that = (MonitorRequest) o;

        if (sourceResourceManagerId != that.sourceResourceManagerId) return false;
        return toMonitor != null ? toMonitor.equals(that.toMonitor) : that.toMonitor == null;

    }

    @Override
    public int hashCode() {
        int result = sourceResourceManagerId;
        result = 31 * result + (toMonitor != null ? toMonitor.hashCode() : 0);
        return result;
    }
}