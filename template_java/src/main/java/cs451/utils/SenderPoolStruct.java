package cs451.utils;

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

public class SenderPoolStruct {

    private List<Set<Packet>> map = new LinkedList<Set<Packet>>();
    // private Map<Integer, Lock> locks = Collections.synchronizedMap(new HashMap<Integer, Lock>());

    // private long[] lastExecution;
    // private boolean[] currentExecution;

    private final int nbins;
    private final long maxSize;
    private long size;

    // private Object locker = new Object();



    public SenderPoolStruct(int nbins, int maxSize) {
        this.size = 0;
        this.nbins = nbins;
        this.maxSize = maxSize;
        // LinkedList<>(); 
        // lastExecution = new long[size];
        // currentExecution = new boolean[size];

        for (int i = 0; i < nbins; i++) {
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

        while (size > maxSize) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.size++;

        Set<Packet> p = null;
        synchronized(map) {
            p = map.get(v);
        }
        synchronized (p) {
            p.add(m);
        }
        
        // synchronized (currentExecution) {
        //     while (currentExecution[v]);
            // map[v].add(m);
        // }
    }

    public synchronized void removeIn(Packet m) {
        int v = m.getMapId();

        // Set<Packet> p = ;
        Set<Packet> p = null;
        synchronized(map) {
            p = map.get(v);
        }
        synchronized (p) {
            p.remove(m);
        }



        this.size--;
        this.notify();
    }

    // public synchronized void clear() {
    //     map.clear();
    //     this.size = 0;
    // }

    public synchronized int getNBins() {
        return nbins;
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

    public synchronized long getTotalSize() {
        return size;
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
