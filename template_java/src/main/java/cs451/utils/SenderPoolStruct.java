package cs451.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cs451.layers.GroundLayer;

public class SenderPoolStruct {
    
    private static final int MAXNUMBEROFCONCURRENTPACKETSPERBIN = ConcurrencyManager.MAXNUMBEROFCONCURRENTPACKETSPERBIN;
    private static final int SENDINGPERIOD = ConcurrencyManager.SENDING_PERIOD_PACKET;
    
    private List<Set<Packet>> map;
    private long size;

    private final int nbins;
    private final long maxSize;
    private final SenderManager manager = new SenderManager();

    public SenderPoolStruct(int nbins, int maxSize) {
        this.nbins = nbins;
        this.maxSize = maxSize;
        this.size = 0;

        map = new ArrayList<>();
        for (int i = 0; i < nbins; i++) {
            map.add(Collections.synchronizedSet(new HashSet<>(MAXNUMBEROFCONCURRENTPACKETSPERBIN * 3)));
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

    public synchronized void sendAll(int v) {
        Set<Packet> p = map.get(v);
       
        if (p.isEmpty()) {
            return;
        }

        for (Packet m : p) {
            GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
        }
    }

    public synchronized void cancelSending(String ip, int port) {
        for (Set<Packet> set : map) {
            List<Packet> packetToRemove = new LinkedList<>();
            for (Packet m : set) {
                if (port != -1 && m.destHost.equals(ip)
                        && m.destPort == port) // cancel messages to a specific host
                {
                    packetToRemove.add(m);
                }
            }
            for (Packet m : packetToRemove) {
                removeIn(m);
            }
        }
    }

    public synchronized long getTotalSize() {
        return  size;
    }

    public synchronized int getNBins() {
        return nbins;
    }

    public synchronized String toString() {
        String s = "(";
        for (int i = 0; i < this.nbins; i++) {
            s += map.get(i).size()/10 + " ";
        }
        s += ")";
        return s;
    }

    private class SenderManager {
        private Timer timerSending = new Timer();
        private Set<Integer> alreadyScheduled = new HashSet<>();

        public synchronized void scheduleSendingPool(int n) {
            if (alreadyScheduled.contains(n)) return;
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
