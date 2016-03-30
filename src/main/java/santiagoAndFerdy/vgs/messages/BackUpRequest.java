package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.gridScheduler.IGridSchedulerGridSchedulerClient;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {
    private @NotNull
    IGridSchedulerGridSchedulerClient primary;
    private @NotNull
    Job jobToBackUp;

    public BackUpRequest(IGridSchedulerGridSchedulerClient source, Job j) {
        this.primary = primary;
        this.jobToBackUp = j;
    }

    public IGridSchedulerGridSchedulerClient getPrimary() {
        return primary;
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
