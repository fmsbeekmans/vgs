package santiagoAndFerdy.vgs.discovery.selector;

import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 4/5/16.
 */
public class LowestWeightSelector implements ISelector {
    public static LowestWeightSelector instance = new LowestWeightSelector();

    private LowestWeightSelector() {
    }

    @Override
    public Optional<Integer> selectIndex(Map<Integer, Long> weights) {
        if(weights.isEmpty()) return Optional.empty();

        long minWeight = Long.MAX_VALUE;
        int minKey = 0;

        for(int k : weights.keySet()) {
            long weight = weights.get(k);

            if(weight < minWeight) {
                minWeight = weight;
                minKey = k;
            }
        }

        return Optional.of(minKey);
    }
}
