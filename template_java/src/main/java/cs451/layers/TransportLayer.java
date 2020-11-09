package cs451.layers;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.utils.Packet;
import cs451.utils.SenderPoolStruct;
import cs451.utils.ConcurrencyManager;

public class TransportLayer {

    private static final int NBINS = ConcurrencyManager.NBINS;
    private static final int GARBAGECOLLECTIONPERIOD = ConcurrencyManager.GARBAGECOLLECTIONPERIOD;
    private static final int MAXNUMBEROFCONCURRENTPACKETS = ConcurrencyManager.MAXNUMBEROFCONCURRENTPACKETSPERBIN * NBINS;

    // logging purposes
    private static final Logger LOG = Logger.getLogger(TransportLayer.class.getName());
    private static final String ORIGIN_STRING = "[TRANSPORT] ";
    private static final String STARTING_STRING = ORIGIN_STRING +  "GC - START collection - toBeSent size: %fk - toBeAcked size: %fk - received size: %fk";
    private static final String END_STRING = ORIGIN_STRING +  "GC - FINISHED collection - toBeSent size: %fk - toBeAcked size: %fk - received size: %fk";

    // layer
    private PerfectLinkLayer perfectLinkLayer = null;

    // variables
    private long seqNum = 0;
    private SenderPoolStruct toBeAcked; 
    private Map<Packet, Long> received = Collections.synchronizedMap(new HashMap<Packet, Long>(2000000));
    private LinkedBlockingQueue<Packet> toBeSent = new LinkedBlockingQueue<Packet>();
    
    // manager and thread stopper
    private GCManager manager = new GCManager();
    private boolean RUNNING = true;


    // Init function
    public void setLayers(PerfectLinkLayer p) {
        this.perfectLinkLayer = p;
        toBeAcked = new SenderPoolStruct(NBINS, MAXNUMBEROFCONCURRENTPACKETS);

        manager.scheduleGC();

        // thread sending the messages
        Thread senderThread = new Thread(() -> senderThreadFunction());
        senderThread.setPriority(Thread.MAX_PRIORITY);
        senderThread.start();
    }

    public void stop() {
        RUNNING = false;
        toBeAcked.clear();
    }

    // function executed by sender thread
    private void senderThreadFunction() {
        while (RUNNING) {
			try {
                Packet m = toBeSent.take(); 
                GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
                toBeAcked.addIn(m); // add to sending pool
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    }

    // function called to enqueue a message
    public void send(String destHost, int destPort, String payload) {
        Packet m = new Packet(destHost, destPort, payload, seqNum);
        seqNum++;
        toBeSent.add(m);
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

    // cancel sending when lost a friend
    public void cancelSending(String ip, int port) {
        toBeAcked.cancelSending(ip, port);
    }

    private class GCManager {
        private Timer timerGC = new Timer();

        public void scheduleGC() {
            TimerTask gcTask = new TimerTask() {
                @Override
                public void run() {
                    LOG.info(String.format(STARTING_STRING, toBeSent.size()/1000.0, toBeAcked.getTotalSize()/1000.0, received.size()/1000.0));
                    List<Packet> toRemove = new LinkedList<>();
                    long currTime = System.currentTimeMillis() - GARBAGECOLLECTIONPERIOD;
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
                    LOG.info(String.format(END_STRING, toBeSent.size()/1000.0, toBeAcked.getTotalSize()/1000.0, received.size()/1000.0));
                }
            };
            this.timerGC.scheduleAtFixedRate(gcTask, 1000, GARBAGECOLLECTIONPERIOD);
        }
    }
}
