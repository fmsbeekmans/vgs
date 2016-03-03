package santiagoAndFerdy.vgs.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by Fydio on 3/4/16.
 */
public class JobTest {
    @Test
    public void getEmptyExtraResourceManagersArrayTest() {
        Job j = new Job(0, 0, 0);

        assertArrayEquals(new int[0], j.getAdditionalResourceManagerIds());
    }

    @Test
    public void hopTest() {
        Job j = new Job(1, 2, 3);

        j.addResourceManagerIds(4);
        assertArrayEquals(new int[] {4}, j.getAdditionalResourceManagerIds());

        j.addResourceManagerIds(5);
        assertArrayEquals(new int[] {4, 5}, j.getAdditionalResourceManagerIds());
    }
}
