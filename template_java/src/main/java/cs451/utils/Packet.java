package cs451.utils;

import cs451.Host;

public class Packet {
    // static constants
    public static String ACK = "ACK";
    public static String PING = "PING";
    public static int DIV1 = ConcurrencyManager.MAXNUMBEROFCONCURRENTPACKETSPERBIN;
    public static int DIV2 = ConcurrencyManager.NBINS;

    // properties of each packet
    public String payload;
    public long seqNum;
    public final String destHost;
    public final int destPort;

    // constructor when receiving
    public Packet(String payload, String h, int p) {
        this.destHost = h;
        this.destPort = p;
        this.seqNum = -1;
        this.payload = payload;
    }
    // to call on received packets
    public void processPacket() {
        String[] splits = payload.split(";", 2);
        this.payload = splits[1];
        this.seqNum = Long.parseLong(splits[0]);
        // System.out.println(this);
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

    public static boolean isPing(String s) {
        return PING.equals(s);
    }

    public String encapsulate() {
        if (isPing()) {
            return PING;
        }
        return seqNum + ";" + payload;
    }

    public String toAckPacket() {
        return seqNum + ";" + ACK;
    }

    public Host getHost() {
        return Host.findHost(this.destHost.toString(), this.destPort);
    }

    @Override
    public String toString() {
        return "(" + new String(destHost) + ";" + destPort + ";" + seqNum + ";" + payload + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
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

    public int getMapId() {
        int number =  (int) (this.seqNum / DIV1) % DIV2;
        if (number < 0) number += DIV2;
        return number;
    }
}
