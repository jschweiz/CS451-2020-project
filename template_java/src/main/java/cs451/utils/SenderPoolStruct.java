package cs451.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import cs451.Host;
import cs451.layers.GroundLayer;

public class SenderPoolStruct {

    private List<Set<Packet>> map;

    private final int nbins;
    private final long maxSize;
    private long size;

    // private Object locker = new Object();
    public static final int MAXNUMBEROFCONCURRENTPACKETSPERBIN = ConcurrencyManager.MAXNUMBEROFCONCURRENTPACKETSPERBIN;


    public SenderPoolStruct(int nbins, int maxSize) {
        this.size = 0;
        this.nbins = nbins;
        this.maxSize = maxSize;
        map = new ArrayList<Set<Packet>>();

        for (int i = 0; i < nbins; i++) {
            map.add(Collections.synchronizedSet(new HashSet<>(MAXNUMBEROFCONCURRENTPACKETSPERBIN * 3)));//(new ConcurrentHashMap(MAXNUMBEROFCONCURRENTPACKETSPERBIN * 4)).newKeySet();
            }
    }

    public synchronized Set<Packet> get(int v) {
        return map.get(v);
    }

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
        map.get(v).add(m);

        manager.scheduleSendingPool(v);
    }

    public synchronized void removeIn(Packet m) {
        int v = m.getMapId();

        Set<Packet> p = map.get(v);
        if (p.contains(m)){
            p.remove(m);
            this.size--;
        }

        this.notify();
    }

    public synchronized void clear() {
        map.clear();
        this.size = 0;
    }

    public synchronized int getNBins() {
        return nbins;
    }

    public synchronized void sendAll(int v) {

        Set<Packet> p = map.get(v);
       
        if (p.isEmpty()) {
            return;
        }

        for (Object o : p) {
            Packet m = (Packet)o;
            GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
        }
        return;
    }

    public synchronized long getTotalSize() {
        return  size;
    }

    public synchronized String toString() {
        String s = "(";
        for (int i = 0; i < this.nbins; i++) {
            s += map.get(i).size()/10 + " ";
        }
        s += ")";
        return s;
    }

    private static final int SENDINGPERIOD = ConcurrencyManager.SENDING_PERIOD_PACKET;
    private SenderManager manager = new SenderManager();

    private class SenderManager {
        private Timer timerSending;
        private Set<Integer> alreadyScheduled = new HashSet<>();

        public SenderManager() {
            this.timerSending = new Timer();
        }

        public synchronized void scheduleSendingPool(int n) {
            if (alreadyScheduled.contains(n))
                return;
            alreadyScheduled.add(n);

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                        sendAll(n);
                }
            };
            this.timerSending.scheduleAtFixedRate(task, SENDINGPERIOD , SENDINGPERIOD);
        }
    }
}
