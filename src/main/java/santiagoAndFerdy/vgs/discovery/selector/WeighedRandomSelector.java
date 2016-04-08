package santiagoAndFerdy.vgs.discovery.selector;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Fydio on 4/5/16.
 */
public class WeighedRandomSelector implements ISelector {
    public static WeighedRandomSelector instance = new WeighedRandomSelector();
    public ThreadLocalRandom rng;

    public WeighedRandomSelector() {
        rng = ThreadLocalRandom.current();
    }

    @Override
    public Optional<Integer> selectIndex(Map<Integer, Long> weights) {
        long n = weights.values().stream().reduce(0L, (a, b) -> a + b);
        n += weights.size();

        Random rng = new Random();
        long random = -1;

        // keep generating a random number until a suitable one is found.
        while (random == -1) {
            long randomTry = new Double(Math.floor(rng.nextDouble() * n)).longValue();
            if(randomTry <= n) random = randomTry;
        }

        int counter = 0;
        Optional<Integer> i = Optional.empty();

        for (int key : weights.keySet()) {
            long weight = weights.get(key);

            if(counter <= random && (counter + weight + 1) > random) {
                i = Optional.of(key);
                break;
            };

            counter += weight + 1;
        }

        return i;
    }
}
