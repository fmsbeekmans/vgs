package santiagoAndFerdy.vgs.discovery.loadBalance;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Created by Fydio on 4/5/16.
 */
public class WeighedRandomSelector implements ISelector {
    public static WeighedRandomSelector instance = new WeighedRandomSelector();

    public WeighedRandomSelector() {
    }

    @Override
    public Optional<Integer> getRandomIndex(Map<Integer, Integer> weights) {
        int sum = weights.values().stream().reduce(0, (a, b) -> a + b);

        Random rng = new Random();

        int random = -1;
        int bound = bits(sum);

        // keep generating a random number until a suitable one is found.
        while (random == -1) {
            int randomTry = rng.nextInt(sum);
            if(randomTry <= sum) random = randomTry;
        }

        int counter = 0;
        Optional<Integer> i = Optional.empty();

        for (int key : weights.keySet()) {
            int weight = weights.get(key);

            if(counter <= random && (counter + weight) > random) {
                i = Optional.of(key);
                break;
            };

            counter += weight;
        }

        return i;
    }

    private static int bits(int of) {
        return new Double(Math.ceil(Math.log(of) / Math.log(2))).intValue();
    }
}
