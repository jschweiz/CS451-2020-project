package cs451.layers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs451.Host;
import cs451.utils.FifoStruc;

public class FIFOLayer {

    // layers and process
    private UBLayer ubLayer;

    private Map<Integer, FifoStruc> received = new HashMap<>(); 
    private int numberOfMessageSent;

    // init functions
    public FIFOLayer() {
        this.numberOfMessageSent = 0;
    }

    public void setLayers(UBLayer b) {
        this.ubLayer = b;
        List<Host> hosts = Host.getHostList();
        for (Host h : hosts) {
            received.put(h.getId(), new FifoStruc());
        }
    }


    // send and receive functions
    public void send(String payload) {
        String augmentedPayload = this.numberOfMessageSent + ";" + payload;
        this.ubLayer.send(augmentedPayload);
        this.numberOfMessageSent++;
    }

    public synchronized void receive(String message, int sender) {
        String[] splits = message.split(";", 2);
        String payload = splits[1];
        int numMessage = Integer.parseInt(splits[0]);
        FifoStruc fStruc = received.get(sender);

        if (fStruc.nextMessageIs(numMessage)) {
            deliver(payload, sender);
            fStruc.receivedMessagePlusOne();
            int counter = numMessage;
            while (fStruc.isAlreadyReceived(++counter)) {
                deliver(fStruc.getMessage(counter), sender);
                fStruc.receivedMessagePlusOne();
            }
        } else {
            fStruc.receiveInAdvance(numMessage, payload);
        }
    }

    private void deliver(String s, int sender) {
        cs451.Process.currProcess.writeInMemory(s, sender, true);
    }
}
