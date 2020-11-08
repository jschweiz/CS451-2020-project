package cs451.layers;

import cs451.Host;
import cs451.Process;
import cs451.TestProcess;

public class PerfectLinkLayer {

    TransportLayer transportLayer;
    BEBLayer bebLayer;

    private int id;

    public PerfectLinkLayer(int id) {
        this.id = id;
    }

    public void setLayers(TransportLayer t, BEBLayer b) {
        this.transportLayer = t;
        this.bebLayer = b;
    }

    public void send(Host destHost, String payload) {
        if (destHost.getId() == this.id) {
            bebLayer.receive(destHost, payload);
        } else {
            transportLayer.send(destHost.getIp(),  destHost.getPort(), payload);
        }
    }

    public void receive(String senderName, int fromPort, String payload) {
        // TestProcess.currProcess.writeInMemory(payload, Host.findHost(senderName, fromPort).getId(), true);
        bebLayer.receive(Host.findHost(senderName, fromPort), payload);
    }    
}
