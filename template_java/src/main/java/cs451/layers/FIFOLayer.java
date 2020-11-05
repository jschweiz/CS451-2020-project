package cs451.layers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs451.utils.Message;
import cs451.Host;
import cs451.utils.AckMap;
import cs451.utils.SynchronizedMessageSet;
import cs451.Process;

public class FIFOLayer {

    private class FifoStruc {
        private Map<Integer, String> messagesWaiting;
        private int lastMessageReceived;

        public FifoStruc() {
            this.lastMessageReceived = -1;
            this.messagesWaiting = new HashMap<>();
        }

        public int getLastMessageNumber() {
            return lastMessageReceived;
        }

        public boolean readyToReceived(int n) {
            return n ==  lastMessageReceived + 1;
        }

        public boolean isAlreadyReceived(int n) {
            return messagesWaiting.containsKey(n);
        }

        public String getMessage(int n) {
            return messagesWaiting.get(n);
        }

        public void receivedOne() {
            lastMessageReceived++;
        }

        public void receive(int n, String s) {
            this.messagesWaiting.put(n, s);
        }
    }

    // layers and process
    private UBLayer ubLayer;
    private Process p;

    private int processID; // used to sign first sender of message

    // used for uniform broadcast

    private Map<Integer, FifoStruc> received = new HashMap<>(); 
    private int numberOfMessageSent;


    // init functions
    public FIFOLayer(int id) {
        this.processID = id;
        this.numberOfMessageSent = 0;
    }

    public void setLayers(UBLayer b, Process p) {
        this.ubLayer = b;
        received.put(this.processID, new FifoStruc());
        List<Host> hosts = PingLayer.currentAvailableHosts();
        for (Host h : hosts) {
            received.put(h.getId(), new FifoStruc());
        }
        this.p = p;
    }


    // send and receive functions
    public synchronized void send(String payload) {

        String augmentedPayload = "N" + this.numberOfMessageSent + ";" + payload;
        this.numberOfMessageSent++;

        // update available host and
        this.ubLayer.send(augmentedPayload);
    }

    public synchronized void receive(String message, int sender) {

        String[] splits = message.split(";");
        String payload = splits[1];
        int numMessage = Integer.parseInt(splits[0].substring(1));
        FifoStruc fStruc = received.get(sender);
        // System.out.println(sender);
        // this.deliver(message, sender, 0);


        if (fStruc.readyToReceived(numMessage)) {
            deliver(payload, sender, numMessage);
            fStruc.receivedOne();
            int counter = numMessage + 1;
            while (fStruc.isAlreadyReceived(counter)) {
                deliver(fStruc.getMessage(counter), sender, counter);
                fStruc.receivedOne();
            }
        } else {
        //     availableHosts.get(sender).put(numMessage, payload);
            fStruc.receive(numMessage, payload);
        }
    }

    public synchronized void deliver(String s, int sender, int numMessage) {
        // this.lastReceived.replace(sender, numMessage-1, numMessage);
        // String pr ="RECEIVED BROADCAST "+ s + " from original process" + sender;
        p.writeInFile(s, this.processID);
    }
}
