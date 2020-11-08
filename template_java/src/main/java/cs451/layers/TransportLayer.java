package cs451.layers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import cs451.utils.Packet;
import cs451.utils.SenderPoolStruct;
import cs451.utils.ConcurrencyManager;

public class TransportLayer {

    private PerfectLinkLayer perfectLinkLayer = null;

    private SenderPoolStruct toBeAcked; 
    private Map<Packet, Long> received = Collections.synchronizedMap(new HashMap<Packet, Long>(2000000));

    private SenderManager manager = new SenderManager();
    private long seqNum = 0;

    private static final int NBINS = ConcurrencyManager.NBINS;
    private static final int GARBAGECOLLECTIONPERIOD = ConcurrencyManager.GARBAGECOLLECTIONPERIOD;

    private static final int MAXNUMBEROFCONCURRENTPACKETS = ConcurrencyManager.MAXNUMBEROFCONCURRENTPACKETSPERBIN * NBINS;

    // Init function
    public void setLayers(PerfectLinkLayer p) {
        this.perfectLinkLayer = p;
        GroundLayer.deliverTo(this);

        toBeAcked = new SenderPoolStruct(NBINS, MAXNUMBEROFCONCURRENTPACKETS);
        manager.scheduleGC();
    }

    public void stop() {
        // toBeAcked.clear();
    }

    // TaskManager to program sending packets
    private class SenderManager {
        private Timer timerGC;

        public SenderManager() {
            this.timerGC = new Timer();
        }

        public void scheduleGC() {
            TimerTask gcTask = new TimerTask() {
                @Override
                public void run() {
                    long currTime = System.currentTimeMillis() - GARBAGECOLLECTIONPERIOD;
                            System.out.println("Starting garbage collection (sizes --> received: "
                                    + (received.size() / 1000.0) + "k  tobeacked: " + (toBeAcked.getTotalSize() / 1000.0) + "k'"
                                    + toBeAcked.getNBins() + ")\n" + toBeAcked);
                        List<Packet> toRemove = new LinkedList<>();
                        synchronized (received) {
                            for (Packet m : received.keySet()) {
                                if (currTime > received.get(m)) {
                                    toRemove.add(m);
                                }
                            }
                        }
                        for (Packet m : toRemove) {
                            received.remove(m);
                        }
                        System.out
                                .println("Garbage collected : " + toRemove.size() + " elements");
                }
            };
            this.timerGC.scheduleAtFixedRate(gcTask, 1000, GARBAGECOLLECTIONPERIOD);
        }
    }

    public void send(String destHost, int destPort, String payload) {
        Packet m = new Packet(destHost, destPort, payload, seqNum);

        // GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
        toBeAcked.addIn(m); // add to sending pool
        seqNum++;
    }
    
    public void receive(Packet m) {
        if (m.isAck()) { // if message is a ACK
                toBeAcked.removeIn(m);
            }
        else { // if normal message
                if (!received.containsKey(m)) { // if not already received
                    received.put(m, System.currentTimeMillis());
                    deliver(m);
                }
            GroundLayer.send(m.toAckPacket(), m.destHost, m.destPort); // reply ack
        }
    }

    private void deliver(Packet m) {
        perfectLinkLayer.receive(m.destHost, m.destPort, m.payload);
    }

    // Cancel sendings 
    public synchronized void cancelSending(String ip, int port) {
            // LinkedList<Packet> packetToRemove = new LinkedList<>();
            // for (int i = 0; i < toBeAcked.getSize(); i++) {
            //     Set<Packet> set = toBeAcked.get(i);
            //     synchronized (set) {
            //         for (Packet m : set) {
            //             if (port != -1 && m.destHost.equals(ip)
            //                     && m.destPort == port) // cancel messages to a specific host
            //             {
            //                 packetToRemove.add(m);
            //             }
            //         }
            //         set.removeAll(packetToRemove);
            //     }
            // }
    }
}
