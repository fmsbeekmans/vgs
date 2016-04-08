package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Fydio on 3/30/16.
 */
public class BackUpRequest implements Serializable {
    private @NotNull WorkRequest workRequest;
    private int[] trail;
    private int backUpsRequested;

    public BackUpRequest(WorkRequest toBackUp, int[] trail, int backUpsRequested) {
        this.workRequest = toBackUp;
        this.trail = trail;
        this.backUpsRequested = backUpsRequested;
    }

    public BackUpRequest(WorkRequest toBackUp, int monitorId, int backUpsRequested) {
        this.workRequest = toBackUp;
        this.trail = new int[1];
        this.trail[0] = monitorId;
        this.backUpsRequested = backUpsRequested;
    }

    public BackUpRequest hop(int at) {
        int n = trail.length;
        int[] newTrail = new int[n + 1];

        for (int i = 0; i < n; i++) newTrail[i] = trail[i];

        newTrail[n] = at;

        return new BackUpRequest(workRequest, newTrail, backUpsRequested - 1);
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public int[] getTrail() {
        return trail;
    }

    public int getBackUpsRequested() {
        return backUpsRequested;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpRequest that = (BackUpRequest) o;

        return workRequest != null ? workRequest.equals(that.workRequest) : that.workRequest == null;
    }
}