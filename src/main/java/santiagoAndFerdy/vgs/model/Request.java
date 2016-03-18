package santiagoAndFerdy.vgs.model;

import com.sun.istack.internal.NotNull;

/**
 * Created by Fydio on 3/19/16.
 * We need to keep track of where the request came from.
 * The job might be processed on a different cluster than it was originally sent to.
 * To keep the 'original' cluster out of the loop we pass a reference to the user respond to
 */
public class Request {
    private Job j;
    private IUser user;

    public Request(@NotNull Job j, @NotNull IUser user) {
        this.j = j;
        this.user = user;
    }

    public Job getJ() {
        return j;
    }

    public IUser getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;

        Request request = (Request) o;

        if (j != null ? !j.equals(request.j) : request.j != null) return false;
        return user != null ? user.equals(request.user) : request.user == null;

    }

    @Override
    public int hashCode() {
        int result = j != null ? j.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
