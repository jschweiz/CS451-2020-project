package cs451.unused;

import java.util.LinkedList;

import cs451.utils.Packet;

public class PacketList {

    private LinkedList<Packet> list;

    public PacketList() {
        this.list = new LinkedList<>();
    }

    public synchronized void add(Packet p) {
        this.list.add(p);
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        return this.list.isEmpty();
    }

    public synchronized Packet removeFirst() {
        while (this.list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return this.list.removeFirst();
    }
}
