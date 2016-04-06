package santiagoAndFerdy.vgs.discovery.selector;

import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 4/5/16.
 */
public interface ISelector {
    /**
     * Select an index based on the given weights
     * @param weights id -> weight
     * @return
     */
    Optional<Integer> getRandomIndex(Map<Integer, Long> weights);
}
