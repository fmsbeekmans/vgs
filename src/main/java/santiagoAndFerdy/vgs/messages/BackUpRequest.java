package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {
    private int sourceGridSchedulerId;
    private @NotNull Job jobToBackUp;

    public BackUpRequest(int sourceResourceManagerId, Job jobToBackUp) {
        this.sourceGridSchedulerId = sourceResourceManagerId;
        this.jobToBackUp = jobToBackUp;
    }

    public int getSourceGridSchedulerId() {
        return sourceGridSchedulerId;
    }

    public Job getJobToBackUp() {
        return jobToBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        if (sourceGridSchedulerId != that.sourceGridSchedulerId) return false;
        return jobToBackUp != null ? jobToBackUp.equals(that.jobToBackUp) : that.jobToBackUp == null;

    }

    @Override
    public int hashCode() {
        int result = sourceGridSchedulerId;
        result = 31 * result + (jobToBackUp != null ? jobToBackUp.hashCode() : 0);
        return result;
    }
}
