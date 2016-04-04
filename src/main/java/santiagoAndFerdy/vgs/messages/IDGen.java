package santiagoAndFerdy.vgs.messages;

public class IDGen {
    private static int ID = 0;

    public static int getNewId(){
        return ID++;
    }
}
