package santiagoAndFerdy.vgs.model;

import java.io.Serializable;

/**
 * Created by Fydio on 3/3/16.
 */
public class Job implements Serializable {
    private int duration;
    private long jobId;
    private long initialResourceManagerId;
    private int[] additionalResourceManagerIds;

    public Job(int duration, long jobId, long initialResourceManagerId) {
        this.duration = duration;
        this.jobId = jobId;
        this.initialResourceManagerId = initialResourceManagerId;
    }

    public int getDuration() {
        return duration;
    }

    public long getJobId() {
        return jobId;
    }

    public long getInitialResourceManagerId() {
        return initialResourceManagerId;
    }

    public int[] getAdditionalResourceManagerIds() {
        return (additionalResourceManagerIds != null) ? additionalResourceManagerIds : new int[0];
    }

    public void addResourceManagerIds(int newResourceManagerId) {
        if (additionalResourceManagerIds == null) {
            additionalResourceManagerIds = new int[] { newResourceManagerId };
        }
        else {
            int n = additionalResourceManagerIds.length;
            int[] swap = new int[n + 1];

            for(int i = 0; i < n; i++) swap[i] = additionalResourceManagerIds[i];

            swap[n] = newResourceManagerId;

            additionalResourceManagerIds = swap;
        }
    }
}