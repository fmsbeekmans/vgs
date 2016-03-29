package santiagoAndFerdy.vgs.messages;

import java.io.Serializable;

/**
 * @author Santi Mar 22, 2016
 */
public class Heartbeat implements Serializable {

    private static final long	serialVersionUID	= -15429184676885602L;
    private long				timestamp;
    private String				content;
    private String				senderURL;
    private String				destURL;

    public Heartbeat(String sender, String dest) {
        this.timestamp = IDGen.getNewId();
        this.content = "ALIVE";
        this.senderURL = sender;
        this.destURL = dest;
    }

    public Heartbeat(String content, String sender, String dest) {
        this.timestamp = IDGen.getNewId();
        this.content = content;
        this.senderURL = sender;
        this.destURL = dest;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    public String getSenderURL() {
        return senderURL;
    }

    public String getDestURL() {
        return destURL;
    }

}