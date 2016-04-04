package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

import com.sun.istack.internal.NotNull;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable, Comparable<BackUpRequest>{

    private static final long serialVersionUID = 6319488637758447075L;
    private int sourceResourceManagerId;
    private @NotNull WorkRequest toBackUp;

    public BackUpRequest(int sourceResourceManagerId, WorkRequest toBackUp) {
        this.sourceResourceManagerId = sourceResourceManagerId;
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

    @Override
    public int compareTo(BackUpRequest o) {
        return toBackUp.compareTo(o.getToBackUp());
    }
}
