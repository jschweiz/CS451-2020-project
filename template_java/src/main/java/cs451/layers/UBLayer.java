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

    private static final int GARBAGECOLLECTIONPERIOD = ConcurrencyManager.GARBAGECOLLECTIONPERIOD;
    private static final boolean GC_ENABLED = ConcurrencyManager.GC_UB_ENABLED;

    // logging purposes
    private static final Logger LOG = Logger.getLogger(UBLayer.class.getName());
    private static final String STARTING_STRING =
        "[UNIFORMB] GC - START collection - delivered size: %fk - forward size: %fk - acked size: %fk";
    private static final String END_STRING =
        "[UNIFORMB] GC - FINISHED collection - delivered size: %fk --> %fk";

    // layers
    private FIFOLayer fifoLayer;
    private BEBLayer bebLayer;

    // variables
    private final int maxConcurrentMessages;
    private final int processID; // used to sign first sender of message
    private int concurrentMessages;
    private Map<Message, Long> delivered = Collections.synchronizedMap(new HashMap<Message, Long>(100000)); //
    private Set<Message> forward = Collections.synchronizedSet(new HashSet<Message>(20000));//
    private AckMap ack = new AckMap();
    private List<Host> availableHosts; // updated directly by pinglayer

    // gc manager
    private GCManager manager = new GCManager(); 


    // init functions
    public UBLayer(int id, int numHosts) {
        this.processID = id;
        this.maxConcurrentMessages = ConcurrencyManager.getUBMaxConcurrentMessages(numHosts);
        System.out.println(maxConcurrentMessages);
        this.concurrentMessages = 0;
    }

    public synchronized void setLayers(BEBLayer b, FIFOLayer f) {
        this.bebLayer = b;
        this.fifoLayer = f;
        this.availableHosts = PingLayer.currentAvailableHosts();
        if (GC_ENABLED) {
            this.manager.scheduleGC();
        }
    }

    // send and receive functions
    public synchronized void send(String payload) {
        Message m = new Message(payload, this.processID);

        while (this.concurrentMessages > maxConcurrentMessages) {
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
        }
    }

    // update current good hosts
    public synchronized void refreshHostList(List<Host> goodHosts) {
        this.availableHosts = goodHosts;
        
        List<Message> messageToDeliver = ack.getAlreadyAckedMessage(this.availableHosts);
        for (Message m : messageToDeliver) {
            deliver(m);
            System.out.println("Juste delivered " + m);
        }
    }

    private synchronized void deliver(Message m) {
        delivered.put(m, System.currentTimeMillis());
        forward.remove(m); 
        this.fifoLayer.receive(m.payload, m.originalSenderId);

        if (m.originalSenderId == this.processID) {
            this.concurrentMessages--;
            if (this.concurrentMessages < maxConcurrentMessages * 0.7) {
                notifyAll();
            }
        }
        // TestProcess.currProcess.writeInMemory(m.payload, m.originalSenderId, true);
    }

    private synchronized void garbageCollect() {
        System.out.println("garbage collect ublayer");
        double deliveredSize =  delivered.size()/1000.0;
        double forwardSize =  forward.size()/1000.0;
        double ackSize =  ack.size()/1000.0;
        LOG.info(String.format(STARTING_STRING, deliveredSize, forwardSize, ackSize));
        
        // long currTime = System.currentTimeMillis() - GARBAGECOLLECTIONPERIOD;
        // List<Message> toRemove = new LinkedList<>();
        // for (Message m : delivered.keySet()) {
        //     if (currTime > delivered.get(m)) {
        //         toRemove.add(m);
        //     }
        // }
        // for (Message m : toRemove) {
        //     delivered.remove(m);
        // }

        // double deliveredNewSize = delivered.size()/1000.0;
        // LOG.info(String.format(END_STRING, deliveredSize, deliveredNewSize));
    }

    private class GCManager {
        private Timer timerGC = new Timer();

        public void scheduleGC() {
            TimerTask gcTask = new TimerTask() {
                @Override
                public void run() {
                    Thread.currentThread().setName("CUSTOM_gc_ublayer_timer");
                    garbageCollect(); 
                }
            };
            this.timerGC.scheduleAtFixedRate(gcTask, GARBAGECOLLECTIONPERIOD * 2, GARBAGECOLLECTIONPERIOD * 2);
        }
    }
}
