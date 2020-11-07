package cs451.utils;

import cs451.Host;

public class Packet {
    // static constants
    public static String ACK = "ACK";
    public static String PING = "PING";

    // properties of each packet
    public final String payload;
    public final long seqNum;
    public final String destHost;
    public final int destPort;

    // constructor when receiving
    public Packet(String payload, String h, int p) {
        String[] splits = payload.split(";", 2);
        this.destHost = h;
        this.destPort = p;
        this.seqNum = Long.valueOf(splits[0]);
        this.payload = splits[1];
    }

    // constructor when sending
    public Packet(String h, int p, String payload, long s) {
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

    public String toAckPacket() {
        return seqNum + ";" + ACK;
    }

    public Host getHost() {
        return Host.findHost(this.destHost, this.destPort);
    }

    @Override
    public String toString() {
        return "(" + destHost + ";" + destPort + ";" + seqNum + ";" + payload + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Packet)) return false;
        Packet m = (Packet)o;
        return seqNum == m.seqNum && destHost.equals(m.destHost)
                && destPort == m.destPort;
    }

    @Override
    public int hashCode() {
        return this.destHost.hashCode() + this.destPort
                + (int)this.seqNum;
    }
}
