package santiagoAndFerdy.vgs.discovery.loadBalance;

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
    public Optional<Integer> getRandomIndex(Map<Integer, Integer> weights) {
        if(weights.isEmpty()) return Optional.empty();

        int minWeight = Integer.MAX_VALUE;
        int minKey = 0;

        for(int k : weights.keySet()) {
            int weight = weights.get(k);

            if(weight < minWeight) {
                minWeight = weight;
                minKey = k;
            }
        }

        return Optional.of(minKey);
    }
}
