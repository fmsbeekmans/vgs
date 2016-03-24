package santiagoAndFerdy.vgs.discovery;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 3/24/16.
 */
public class AppendableRepositoryTest extends RepositoryTest {
    @Test
    public void canAppendRepository(){
        AppendableRepository<IRemoteMock> repo = (AppendableRepository<IRemoteMock>) getRepository(new HashMap<>());
        repo.put(3, "new user");

        assertTrue(repo.ids().contains(3));
        assertEquals(Optional.of(Status.OFFLINE), repo.getLastKnownStatus(3));
    }

    @Override
    public IRepository<IRemoteMock> getRepository(Map<Integer, String> entries) {
        return new AppendableRepository<>(entries);
    }
}
