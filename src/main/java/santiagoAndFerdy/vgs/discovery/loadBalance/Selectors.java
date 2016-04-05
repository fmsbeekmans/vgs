package santiagoAndFerdy.vgs.discovery.loadBalance;

/**
 * Created by Fydio on 4/5/16.
 */
public class Selectors {
    public static ISelector lowest = LowestWeightSelector.instance;
    public static ISelector weighedRandom = WeighedRandomSelector.instance;
}
