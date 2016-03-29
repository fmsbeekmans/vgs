package santiagoAndFerdy.vgs.messages;

public class IDGen {
    private static long ID = 0;

    public static long getNewId(){
        return ID++;
    }
}
