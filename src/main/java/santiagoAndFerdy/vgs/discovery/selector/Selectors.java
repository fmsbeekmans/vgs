package santiagoAndFerdy.vgs.discovery.selector;

/**
 * Created by Fydio on 4/5/16.
 */
public class Selectors {
    public static ISelector lowest = LowestWeightSelector.instance;
    public static ISelector weighedRandom = WeighedRandomSelector.instance;
    public static ISelector invertedWeighedRandom = InvertedWeighedRandomSelector.instance;
}
