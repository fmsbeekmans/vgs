package santiagoAndFerdy.vgs.model.cluster;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;

import java.util.Optional;

/**
 * Created by Fydio on 3/19/16.
 */
public class Node {
    private int id;
    private Job job;
    private IResourceManagerDriver rm;

    public Node(int id, @NotNull IResourceManagerDriver rm) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        if (id != node.id) return false;
        return rm.equals(node.rm);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + rm.hashCode();
        return result;
    }
}
