package santiagoAndFerdy.vgs.messages;

import com.sun.istack.internal.NotNull;
import santiagoAndFerdy.vgs.model.Job;
import santiagoAndFerdy.vgs.resourceManager.IResourceManagerUserClient;

import java.io.Serializable;

/**
 * Created by Fydio on 3/19/16.
 * We need to keep track of where the request came from.
 * The job might be processed on a different cluster than it was originally sent to.
 * To keep the 'original' cluster out of the loop we pass a reference to the user respond to
 */
public class UserRequest implements Serializable {
    private Job j;
    private IResourceManagerUserClient user;

    public UserRequest(@NotNull Job j, @NotNull IResourceManagerUserClient endPoint) {
        this.j = j;
        this.user = endPoint;
    }

    public Job getJob() {
        return j;
    }

    public IResourceManagerUserClient getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRequest)) return false;

        UserRequest userRequest = (UserRequest) o;

        if (j != null ? !j.equals(userRequest.j) : userRequest.j != null) return false;
        return user != null ? user.equals(userRequest.user) : userRequest.user == null;
    }

    @Override
    public int hashCode() {
        int result = j != null ? j.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}