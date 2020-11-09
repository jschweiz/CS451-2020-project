package cs451.unused;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cs451.Host;
import cs451.layers.GroundLayer;
import cs451.utils.ConcurrencyManager;
import cs451.utils.Packet;

public class ListPacketSet {

    private List<Set<Packet>> map = new LinkedList<Set<Packet>>();
    // private Map<Integer, Lock> locks = Collections.synchronizedMap(new HashMap<Integer, Lock>());

    // private long[] lastExecution;
    // private boolean[] currentExecution;

    private int size;
    private long mSize;

    // private Object locker = new Object();

    private static int INTERVAL = ConcurrencyManager.SENDING_PERIOD_PACKET;

    private long maxSize;

    public ListPacketSet(int size, int maxSize) {
        this.mSize = 0;
        this.size = size;
        this.maxSize = maxSize;
        // LinkedList<>(); 
        // lastExecution = new long[size];
        // currentExecution = new boolean[size];

        for (int i = 0; i < size; i++) {
            // currentExecution[i] = false;
            map.add(Collections.synchronizedSet(new HashSet<Packet>()));
            // locks.put(i, new ReentrantLock());
        }

        // for (int i = 0; i < size; i++) {
        //     map.add(new HashSet<Packet>());
        //     lastExecution.add((long)0);
        // }
    }

    // public synchronized Set<Packet> get(int v) {
    //     Set<Packet> set = null;
    //     synchronized (map) {
    //         set = map[v];
    //     }
    //     return set;
    // }

    public synchronized void addIn(Packet m) {
        int v = m.getMapId();

        while (mSize > maxSize) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.mSize++;

        map.get(v).remove(m);
        
        // synchronized (currentExecution) {
        //     while (currentExecution[v]);
            // map[v].add(m);
        // }
    }

    public synchronized void removeIn(Packet m) {
        int v = m.getMapId();

        // Set<Packet> p = ;

        map.get(v).remove(m);
        this.mSize--;
        this.notify();
    }

    // public synchronized void clear() {
    //     map.clear();
    //     this.size = 0;
    // }

    public synchronized int getSize() {
        return size;
    }

    public void sendAll(int v) {

        // synchronized(currentExecution) {
        //     if (currentExecution[v]) {
        //         return;
        //     } else {
        //         currentExecution[v] = true;
        //     }
        // }

        // synchronized(lastExecution) {
        //     lastExecution[v] = System.currentTimeMillis();
        // }

        Set<Packet> p = null;
        
        synchronized (map) {
            p =  map.get(v);
        }
       

        if (p.isEmpty()) {
            // synchronized(lastExecution) {
            //     lastExecution[v] = System.currentTimeMillis();
            // }
            return;
        }

        synchronized(p) {
            for (Packet m : p) {
                GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
            }
        }

        // synchronized(lastExecution) {
        //     lastExecution[v] = System.currentTimeMillis();
        // }

        // synchronized(currentExecution) {
        //     currentExecution[v] = false;
        // }
        return;
    }

    public synchronized long totalSize() {
        return mSize;
    }

    public synchronized String toString() {
        String s = "(";
        // for (int i = 0; i < this.size; i++) {
        //     s += map[i].size()/10 + " ";
        // }
        // s += ")";
        return s;
    }
}
