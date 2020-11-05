package cs451.utils;

public class Message {
    public String payload;
    public int originalSenderId;

    public Message(String p, int id) {
        this.payload = p;
        this.originalSenderId = id;
    }
    public Message(String n) {
        String[] split = n.split(";", 2);
        this.payload = split[1];
        this.originalSenderId = Integer.valueOf(split[0]);
    }
    public String toString() {
        return originalSenderId + ";" +  payload;
    }
    public boolean equals(Object o) {
        if (!(o instanceof Message)) return false;
        Message m = (Message) o;
        return payload.equals(m.payload) && originalSenderId == m.originalSenderId;
    }
    public int hashCode() {
        return payload.hashCode() + 11 * originalSenderId;
    }
}
