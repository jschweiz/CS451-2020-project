package cs451.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cs451.Host;
import cs451.layers.GroundLayer;

public class ListPacketSet {

    private List<Set<Packet>> map = new LinkedList<>();
    private int size;
    private long mSize;

    private long maxSize;

    public ListPacketSet(int size, int maxSize) {
        this.mSize = 0;
        this.size = size;
        this.maxSize = maxSize;
        for (int i = 0; i < size; i++) {
            map.add(Collections.synchronizedSet(new HashSet<Packet>()));
        }
    }

    public synchronized Set<Packet> get(int v) {
        return map.get(v);
    }

    public synchronized void addIn(Packet m) {

        while (mSize > maxSize) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.mSize++;
        map.get(m.getMapId()).add(m);
    }

    public synchronized void removeIn(Packet m) {
        this.mSize--;
        map.get(m.getMapId()).remove(m);
        this.notify();
    }

    public synchronized void clear() {
        map.clear();
        this.size = 0;
    }

    public synchronized int getSize() {
        return size;
    }

    public synchronized boolean sendAll(int v) {

        Set<Packet> set = null;
        synchronized (map) {
            set =  map.get(v);
        }
        if (set.isEmpty()) return true;

        for (Packet m : set) {
            GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
        }
        return false;
    }

    public synchronized long totalSize() {
        return mSize;
    }

    public synchronized String toString() {
        String s = "(";
        for (int i = 0; i < this.size; i++) {
            s += map.get(i).size()/10 + " ";
        }
        s += ")";
        return s;
    }
}
