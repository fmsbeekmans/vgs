package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

/**
 * Created by Fydio on 4/6/16.
 */
public class WorkOrder implements Serializable, Comparable<WorkOrder> {
    private static final long serialVersionUID = 9214813329697464666L;
    
    private int fromGridSchedulerId;
    private WorkRequest workRequest;

    public WorkOrder(int fromGridSchedulerId, WorkRequest workRequest) {
        this.fromGridSchedulerId = fromGridSchedulerId;
        this.workRequest = workRequest;
    }

    public int getFromGridSchedulerId() {
        return fromGridSchedulerId;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkOrder workOrder = (WorkOrder) o;

        if (fromGridSchedulerId != workOrder.fromGridSchedulerId) return false;
        return workRequest != null ? workRequest.equals(workOrder.workRequest) : workOrder.workRequest == null;

    }

    @Override
    public int hashCode() {
        int result = fromGridSchedulerId;
        result = 31 * result + (workRequest != null ? workRequest.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(WorkOrder o) {
        return this.workRequest.compareTo(o.workRequest);
    }
}
