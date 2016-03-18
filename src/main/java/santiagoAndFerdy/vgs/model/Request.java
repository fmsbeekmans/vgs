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
}
