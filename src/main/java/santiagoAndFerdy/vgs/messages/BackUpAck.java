package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Fydio on 4/7/16.
 */
public class BackUpAck implements Serializable {
    private WorkRequest workRequest;
    private int[] backUps;

    public BackUpAck(WorkRequest workRequest, int... backUps) {
        this.workRequest = workRequest;
        this.backUps = backUps;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public int[] getBackUps() {
        return backUps;
    }

    public BackUpAck prependGridSchedulerList(int with) {
        int n = backUps.length;
        int[] newIds = new int[n + 1];
        newIds[0] = with;

        for (int i = 0; i < n; i++) newIds[i + 1] = backUps[i];

        return new BackUpAck(workRequest, newIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpAck backUpAck = (BackUpAck) o;

        if (workRequest != null ? !workRequest.equals(backUpAck.workRequest) : backUpAck.workRequest != null)
            return false;
        return Arrays.equals(backUps, backUpAck.backUps);

    }

    @Override
    public int hashCode() {
        int result = workRequest != null ? workRequest.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(backUps);
        return result;
    }
}
