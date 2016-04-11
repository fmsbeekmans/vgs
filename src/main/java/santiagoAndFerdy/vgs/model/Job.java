package santiagoAndFerdy.vgs.model;

import java.io.Serializable;

/**
 * Created by Fydio on 3/3/16.
 */
public class Job implements Serializable, Comparable<Job> {

    private static final long serialVersionUID = 666864530383448502L;
    private int               duration;
    private int               jobId;
    private int               initialResourceManagerId;
    private int[]             additionalResourceManagerIds;
    private int               size;
    private long              startTime;
    private int               currentResourceManagerId;

    public Job(int duration, int jobId, int initialResourceManagerId) {
        this.duration = duration;
        this.jobId = jobId;
        this.initialResourceManagerId = initialResourceManagerId;
        currentResourceManagerId = initialResourceManagerId;
        startTime = System.currentTimeMillis();
    }

    public int getDuration() {
        return duration;
    }
    
    public long getStartTime() {
        return startTime;
    }

    public int getJobId() {
        return jobId;
    }

    public int getInitialResourceManagerId() {
        return initialResourceManagerId;
    }

    public int[] getAdditionalResourceManagerIds() {
        return (additionalResourceManagerIds != null) ? additionalResourceManagerIds : new int[0];
    }

    public void addResourceManagerId(int newResourceManagerId) {
        if (additionalResourceManagerIds == null) {
            additionalResourceManagerIds = new int[5];
            additionalResourceManagerIds[0] = newResourceManagerId;
            size = 1;
        } else {
            if (size == additionalResourceManagerIds.length) {
                int n = additionalResourceManagerIds.length;
                int[] swap = new int[n * 2];
                for (int i = 0; i < n; i++)
                    swap[i] = additionalResourceManagerIds[i];

                swap[n] = newResourceManagerId;

                additionalResourceManagerIds = swap;
                size++;
            } else {
                additionalResourceManagerIds[size] = newResourceManagerId;
                size++;
            }
            currentResourceManagerId = newResourceManagerId;
        }
    }

    public int getCurrentResourceManagerId() {
        return currentResourceManagerId;
    }

    @Override
    public int compareTo(Job o) {
        return jobId - o.getJobId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Job job = (Job) o;

        if (jobId != job.jobId)
            return false;
        return initialResourceManagerId == job.initialResourceManagerId;
    }

    @Override
    public int hashCode() {
        int result = (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + (int) (initialResourceManagerId ^ (initialResourceManagerId >>> 32));
        return result;
    }
}
