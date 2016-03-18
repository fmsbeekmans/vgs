package santiagoAndFerdy.vgs.model;

/**
 * Created by Fydio on 3/19/16.
 */
public class Request {
    private Job j;
    private IUser user;

    public Request(Job j, IUser user) {
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
