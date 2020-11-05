package cs451.utils;

public class Packet {
    public String payload;
    public int seqNum;
    public String destHost;
    public int destPort;

    public static String ACK = "ACK";
    public static String PING = "PING";

    public Packet(String payload, String h, int p) {
        this(h, p, payload.split(";", 2)[1], Integer.valueOf(payload.split(";", 2)[0]));
    }

    public Packet(String h, int p, String payload, int s) {
        this.destHost = h;
        this.destPort = p;
        this.seqNum = s;
        this.payload = payload;
    }

    public boolean isAck() {
        return ACK.equals(payload);
    }

    public boolean isPing() {
        return PING.equals(payload);
    }

    public String encapsulate() {
        return seqNum + ";" + payload;
    }

    @Override
    public String toString() {
        return "(" + destHost + ";" + destPort + ";" + seqNum + ";" + payload + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Packet))
            return false;
        Packet m = (Packet)o;
        return seqNum == m.seqNum && destHost.equals(m.destHost)
                && destPort == m.destPort;
    }

    @Override
    public int hashCode() {
        return this.destHost.hashCode() + this.destPort
                + this.seqNum;
    }
}
