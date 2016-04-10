package santiagoAndFerdy.vgs.discovery.selector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Fydio on 4/9/16.
 */
public class InvertedWeighedRandomSelector implements ISelector {
    public static ISelector instance = new InvertedWeighedRandomSelector();

    private InvertedWeighedRandomSelector() {
    }

    @Override
    public Optional<Integer> selectIndex(Map<Integer, Long> weights) {
        return weights.values().stream().max(Long::compareTo).map(max -> {
            Map<Integer, Long> invertedWeights = new HashMap<Integer, Long>();
            weights.forEach((id, weight) -> {
                invertedWeights.put(id, max - weight);
            });

            return invertedWeights;
        }).flatMap(Selectors.weighedRandom::selectIndex);
    }
}
