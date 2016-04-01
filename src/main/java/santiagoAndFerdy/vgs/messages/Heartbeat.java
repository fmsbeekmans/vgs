package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

/**
 * @author Santi Mar 22, 2016
 */
public class Heartbeat implements Serializable {

    private static final long	serialVersionUID	= -15429184676885602L;
    private long				timestamp;
    private String				destURL;

    public Heartbeat(String dest) {
        this.timestamp = IDGen.getNewId();
        this.destURL = dest;
    }

    public long getTimestamp() {
        return timestamp;
    }



    public String getDestURL() {
        return destURL;
    }

}