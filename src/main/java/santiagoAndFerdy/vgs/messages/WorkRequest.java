package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

import santiagoAndFerdy.vgs.model.Job;

/**
 * Created by Fydio on 4/3/16.
 */
public class WorkRequest implements Serializable{
    private static final long serialVersionUID = -1673621408938958506L;

    private String userUrl;

    private Job toExecute;
    public WorkRequest(){
        
    }
    public WorkRequest(String userUrl, Job toExecute) {
        this.userUrl = userUrl;
        this.toExecute = toExecute;
    }

    public String getUserUrl() {
        return userUrl;
    }

    public Job getJob() {
        return toExecute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkRequest that = (WorkRequest) o;

        if (userUrl != null ? !userUrl.equals(that.userUrl) : that.userUrl != null) return false;
        return toExecute != null ? toExecute.equals(that.toExecute) : that.toExecute == null;

    }

    @Override
    public int hashCode() {
        int result = userUrl != null ? userUrl.hashCode() : 0;
        result = 31 * result + (toExecute != null ? toExecute.hashCode() : 0);
        return result;
    }
}
