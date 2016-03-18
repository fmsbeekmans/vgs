package santiagoAndFerdy.vgs.model.cluster;

import santiagoAndFerdy.vgs.model.Job;

import java.util.Optional;

/**
 * Created by Fydio on 3/19/16.
 */
public class Node {
    private int id;
    private Job job;
    private IResourceManagerDriver rm;

    public Node(int id, IResourceManagerDriver rm) {
        this.id = id;
        this.rm = rm;
    }

    public int getId() {
        return id;
    }

    public Optional<Job> getJob() {
        return Optional.ofNullable(job);
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public IResourceManagerDriver getRm() {
        return rm;
    }
}
