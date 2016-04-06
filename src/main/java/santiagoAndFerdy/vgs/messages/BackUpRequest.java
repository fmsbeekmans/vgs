package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {

    private static final long serialVersionUID = 6319488637758447075L;
    private int sourceResourceManagerId;
    private @NotNull WorkRequest toBackUp;

    public BackUpRequest(int sourceGridSchedulerId, WorkRequest toBackUp) {
        this.sourceResourceManagerId = sourceGridSchedulerId;
        this.toBackUp = toBackUp;
    }

    public int getSourceResourceManagerId() {
        return sourceResourceManagerId;
    }

    public WorkRequest getToBackUp() {
        return toBackUp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        if (sourceResourceManagerId != that.sourceResourceManagerId) return false;
        return toBackUp != null ? toBackUp.equals(that.toBackUp) : that.toBackUp == null;

    }

    @Override
    public int hashCode() {
        int result = sourceResourceManagerId;
        result = 31 * result + (toBackUp != null ? toBackUp.hashCode() : 0);
        return result;
    }
}