package santiagoAndFerdy.vgs;

import santiagoAndFerdy.vgs.discovery.selector.Selectors;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by Fydio on 4/5/16.
 */
public class WeighedRandomSelectorTest {
    public static void main(String[] args) {
        Map<Integer, Long> weights = new HashMap<>();

        weights.put(1, 3L);
        weights.put(2, 5L);
        weights.put(3, 2L);
        weights.put(4, 10L);

        Map<Integer, Integer> results = new HashMap<>();

        results.put(1, 0);
        results.put(2, 0);
        results.put(3, 0);
        results.put(4, 0);

        for (int i = 0; i < 2000; i++) {
            int key = Selectors.invertedWeighedRandom.selectIndex(weights).get();

            results.put(key, results.get(key) + 1);
        }

        for(int key : results.keySet()) {
            System.out.println(key + ": " + results.get(key));
        }
    }
}
