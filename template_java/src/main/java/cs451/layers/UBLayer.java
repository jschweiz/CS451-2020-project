package cs451.layers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cs451.utils.Message;
import cs451.Host;
import cs451.utils.AckMap;
import cs451.utils.SynchronizedMessageSet;
import cs451.Process;

public class UBLayer {

    // layers and process
    private FIFOLayer fifoLayer;
    private BEBLayer bebLayer;
    private Process p;

    private int processID; // used to sign first sender of message

    // used for uniform broadcast
    private Set<Message> delivered = Collections.synchronizedSet(new HashSet<Message>());
    private SynchronizedMessageSet forward = new SynchronizedMessageSet();
    private AckMap ack = new AckMap();

    private List<Host> availableHosts; // updated directly by pinglayer 


    // init functions
    public UBLayer(int id) {
        this.processID = id;
    }

    public void setLayers(BEBLayer b, FIFOLayer f, Process p) {
        this.bebLayer = b;
        this.fifoLayer = f;
        this.availableHosts = PingLayer.currentAvailableHosts();
        this.p = p;
    }


    // send and receive functions
    public synchronized void send(String payload) {
        Message m = new Message(payload, this.processID);

        // add the message to forward
        forward.add(m);

        // update available host and
        this.bebLayer.send(m, this.availableHosts);
    }

    public synchronized void receive(Host sender, Message m) {
        if (delivered.contains(m)) {
            System.out.println(".......");
            return;
        }
        ack.ackBy(m, sender);

        if (!forward.contains(m)) {
            forward.addForced(m);
            this.bebLayer.send(m, this.availableHosts);
        }

        if (ack.allAcked(m, availableHosts)) {
            deliver(m);
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

    public void deliver(Message m) {
        delivered.add(m);
        forward.remove(m);

        this.fifoLayer.receive(m.payload, m.originalSenderId);

        // p.writeInFile(m.payload, this.processID);
    }
}
