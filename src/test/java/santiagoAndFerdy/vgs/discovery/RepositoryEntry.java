package santiagoAndFerdy.vgs.discovery;

import java.rmi.Remote;

/**
 * Created by Fydio on 3/24/16.
 */
class RepositoryEntry<T extends Remote> {
    private int id;
    private String url;
    private T entity;

    public RepositoryEntry(int id, String url, T entity) {
        this.id = id;
        this.url = url;
        this.entity = entity;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public T getEntity() {
        return entity;
    }
}
