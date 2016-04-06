package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

/**
 * Created by Fydio on 4/6/16.
 */
public class PromotionRequest implements Serializable {

    private int sourceResourceManagerId;
    private WorkRequest toBecomePrimaryFor;

    public PromotionRequest(int sourceResourceManagerId, WorkRequest toBackUp) {
        this.sourceResourceManagerId = sourceResourceManagerId;
        this.toBecomePrimaryFor = toBackUp;
    }

    public int getSourceResourceManagerId() {
        return sourceResourceManagerId;
    }

    public WorkRequest getToBecomePrimaryFor() {
        return toBecomePrimaryFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PromotionRequest that = (PromotionRequest) o;

        if (sourceResourceManagerId != that.sourceResourceManagerId) return false;
        return toBecomePrimaryFor != null ? toBecomePrimaryFor.equals(that.toBecomePrimaryFor) : that.toBecomePrimaryFor == null;

    }

    @Override
    public int hashCode() {
        int result = sourceResourceManagerId;
        result = 31 * result + (toBecomePrimaryFor != null ? toBecomePrimaryFor.hashCode() : 0);
        return result;
    }
}
