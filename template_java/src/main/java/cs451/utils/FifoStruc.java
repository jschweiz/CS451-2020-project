package cs451.utils;

import java.util.HashMap;
import java.util.Map;

public class FifoStruc {
    private Map<Integer, String> messagesWaiting;
    private int lastMessageReceived;

    public FifoStruc() {
        this.lastMessageReceived = -1;
        this.messagesWaiting = new HashMap<>(1000000);
    }

    public boolean nextMessageIs(int n) {
        return n ==  lastMessageReceived + 1;
    }

    public boolean isAlreadyReceived(int n) {
        return messagesWaiting.containsKey(n);
    }

    public String getMessage(int n) {
        String s = messagesWaiting.get(n);
        messagesWaiting.remove(n);
        return s;
    }

    public void receivedMessagePlusOne() {
        lastMessageReceived++;
    }

    public void receiveInAdvance(int n, String s) {
        this.messagesWaiting.put(n, s);
    }

    public String toString() {
        String s = "FIFOSTRUCT: " + lastMessageReceived+ "\n ";
        for (Integer i : messagesWaiting.keySet()) {
            s+= "  -  " + i + " :: " + messagesWaiting.get(i) + "//  \n";
        }
        return s;
    }
}
