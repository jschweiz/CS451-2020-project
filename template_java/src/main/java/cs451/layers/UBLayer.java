package cs451.layers;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cs451.utils.Message;
import cs451.Host;
import cs451.utils.AckMap;
import cs451.utils.ConcurrencyManager;

public class UBLayer {

    private static final int MAX_CONCURRENT_MESSAGES = ConcurrencyManager.MAX_CONCURRENT_MESSAGES;
    private static final int GARBAGECOLLECTIONPERIOD = ConcurrencyManager.GARBAGECOLLECTIONPERIOD;

    // logging purposes
    private static final Logger LOG = Logger.getLogger(UBLayer.class.getName());
    private static final String STARTING_STRING =
        "[UNIFORMB] GC - START collection - delivered size: %fk - forward size: %fk - acked size: %fk";
    private static final String END_STRING =
        "[UNIFORMB] GC - FINISHED collection - delivered size: %fk - forward size: %fk - acked size: %fk";

    // layers
    private FIFOLayer fifoLayer;
    private BEBLayer bebLayer;

    // variables
    private final int processID; // used to sign first sender of message
    private int concurrentMessages;
    private Map<Message, Long> delivered = Collections.synchronizedMap(new HashMap<Message, Long>(2000000));
    private Set<Message> forward = Collections.synchronizedSet(new HashSet<Message>(2000000));
    private AckMap ack = new AckMap();
    private List<Host> availableHosts; // updated directly by pinglayer

    // gc manager
    private GCManager manager = new GCManager(); 


    // init functions
    public UBLayer(int id) {
        this.processID = id;
        this.concurrentMessages = 0;
    }

    public void setLayers(BEBLayer b, FIFOLayer f) {
        this.bebLayer = b;
        this.fifoLayer = f;
        this.availableHosts = PingLayer.currentAvailableHosts();
        this.manager.scheduleGC();
    }

    // send and receive functions
    public synchronized void send(String payload) {
        Message m = new Message(payload, this.processID);

        while (this.concurrentMessages > MAX_CONCURRENT_MESSAGES) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // add the message to forward
        forward.add(m);

        // update available host and
        this.bebLayer.send(m, this.availableHosts);

        this.concurrentMessages++;
    }

    public synchronized void receive(Host sender, Message m) {
        if (delivered.containsKey(m)) {
            return;
        }
        ack.ackBy(m, sender);

        if (!forward.contains(m)) {
            forward.add(m);
            this.bebLayer.send(m, this.availableHosts);
        }

        if (ack.allAcked(m, availableHosts)) {
            deliver(m);
            
        if (m.originalSenderId == this.processID) {
                this.concurrentMessages--;
                if (this.concurrentMessages < MAX_CONCURRENT_MESSAGES * 0.7) {
                    notify();
                }
            }
        }
    }

    // update current good hosts
    public synchronized void lostConnectionTo(List<Host> goodHosts) {
        this.availableHosts = goodHosts;
 
        for (Message m : ack.getAllMessages()) {
            if (ack.allAcked(m, availableHosts)) {
                deliver(m);
            }
        }
    }

    private synchronized void deliver(Message m) {
        delivered.put(m, System.currentTimeMillis());
        forward.remove(m);
        this.fifoLayer.receive(m.payload, m.originalSenderId);
        // TestProcess.currProcess.writeInMemory(m.payload, m.originalSenderId, true);
    }


    private class GCManager {
        private Timer timerGC = new Timer();

        public void scheduleGC() {
            TimerTask gcTask = new TimerTask() {
                @Override
                public void run() {
                    LOG.info(String.format(STARTING_STRING, delivered.size()/1000.0, forward.size()/1000.0, ack.size()/1000.0));
                    
                    long currTime = System.currentTimeMillis() - GARBAGECOLLECTIONPERIOD;
                    List<Message> toRemove = new LinkedList<>();
                    synchronized (delivered) {
                        for (Message m : delivered.keySet()) {
                            if (currTime > delivered.get(m)) {
                                toRemove.add(m);
                            }
                        }
                    }
                    for (Message m : toRemove) {
                        delivered.remove(m);
                    }

                    LOG.info(String.format(END_STRING, delivered.size()/1000.0,
                            forward.size()/1000.0, ack.size()/1000.0));
                }
            };
            this.timerGC.scheduleAtFixedRate(gcTask, GARBAGECOLLECTIONPERIOD * 3, GARBAGECOLLECTIONPERIOD * 3);
        }
    }
}
