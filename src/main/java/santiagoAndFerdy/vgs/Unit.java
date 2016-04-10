package santiagoAndFerdy.vgs;

/**
 * Created by Fydio on 4/10/16.
 */
public class Unit {
    public static Unit instance = new Unit();

    private Unit() {}

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Unit;
    }
}
