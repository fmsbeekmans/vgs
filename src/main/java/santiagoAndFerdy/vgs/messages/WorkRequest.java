package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

import santiagoAndFerdy.vgs.model.Job;

/**
 * Created by Fydio on 4/3/16.
 */
public class WorkRequest implements Serializable, Comparable<WorkRequest>{
    private static final long serialVersionUID = -1673621408938958506L;

    private int userId;
    private Job job;

    public WorkRequest(int userId, Job toExecute) {
        this.userId = userId;
        this.job = toExecute;
    }

    public int getUserId() {
        return userId;
    }

    public Job getJob() {
        return job;
    }

    @Override
    public int compareTo(WorkRequest o) {
        return job.compareTo(o.job);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkRequest that = (WorkRequest) o;

        if (userId != that.userId) return false;
        return job != null ? job.equals(that.job) : that.job == null;

    }

    @Override
    public int hashCode() {
        int result = userId;
        result = 31 * result + (job != null ? job.hashCode() : 0);
        return result;
    }
}
