package cs451.layers;
import cs451.Host;
import cs451.Process;

public class PerfectLinkLayer {

    TransportLayer transportLayer;
    BEBLayer bebLayer;
    Process p;

    public void setLayers(TransportLayer t, BEBLayer b, Process p) {
        this.transportLayer = t;
        this.bebLayer = b;
        this.p = p;
    }

    public void send(Host destHost, String payload) {
        transportLayer.send(destHost.getIp(),  destHost.getPort(), payload);
    }

    public void receive(String senderName, int fromPort, String payload) {
        bebLayer.receive(Host.findHost(senderName, fromPort), payload);
        // p.writeInFile(payload);
    }    
}
