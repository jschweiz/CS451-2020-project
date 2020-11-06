package cs451.layers;
import cs451.Host;

public class PerfectLinkLayer {

    TransportLayer transportLayer;
    BEBLayer bebLayer;

    public void setLayers(TransportLayer t, BEBLayer b) {
        this.transportLayer = t;
        this.bebLayer = b;
    }

    public void send(Host destHost, String payload) {
        transportLayer.send(destHost.getIp(),  destHost.getPort(), payload);
    }

    public void receive(String senderName, int fromPort, String payload) {
        bebLayer.receive(Host.findHost(senderName, fromPort), payload);
    }    
}
