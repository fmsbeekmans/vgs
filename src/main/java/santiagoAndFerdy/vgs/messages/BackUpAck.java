package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

/**
 * Created by Fydio on 4/7/16.
 */
public class BackUpAck implements Serializable {
    private WorkRequest workRequest;
    private int[] backUps;

    public BackUpAck(WorkRequest workRequest, int[] backUps) {
        this.workRequest = workRequest;
        this.backUps = backUps;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public int[] getBackUps() {
        return backUps;
    }

    public BackUpAck appendGridSchedulerList(int with) {
        int n = backUps.length;
        int[] newIds = new int[n + 1];

        for (int i = 0; i < n; i++) newIds[i] = backUps[i];
        newIds[n] = with;

        return new BackUpAck(workRequest, newIds);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackUpAck backUpAck = (BackUpAck) o;

        return workRequest != null ? workRequest.equals(backUpAck.workRequest) : backUpAck.workRequest == null;

    }

    @Override
    public int hashCode() {
        return workRequest != null ? workRequest.hashCode() : 0;
    }
}
