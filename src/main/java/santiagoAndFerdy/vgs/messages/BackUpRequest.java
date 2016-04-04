package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {

    private static final long serialVersionUID = 6319488637758447075L;
    private int sourceGridSchedulerId;
    private @NotNull WorkRequest toBackUp;

    public BackUpRequest(int sourceGridSchedulerId, WorkRequest toBackUp) {
        this.sourceGridSchedulerId = sourceGridSchedulerId;
        this.toBackUp = toBackUp;
    }

    public int getSourceGridSchedulerId() {
        return sourceGridSchedulerId;
    }

    public WorkRequest getToBackUp() {
        return toBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        if (sourceGridSchedulerId != that.sourceGridSchedulerId) return false;
        return toBackUp != null ? toBackUp.equals(that.toBackUp) : that.toBackUp == null;

    }

    @Override
    public int hashCode() {
        int result = sourceGridSchedulerId;
        result = 31 * result + (toBackUp != null ? toBackUp.hashCode() : 0);
        return result;
    }
}