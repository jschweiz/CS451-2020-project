package cs451.layers;

import java.util.List;

import cs451.Host;
import cs451.utils.Message;

public class BEBLayer {

    private PerfectLinkLayer perfectLinkLayer;
    private UBLayer bLayer;

    public void setLayers(PerfectLinkLayer pe, UBLayer b) {
        this.perfectLinkLayer = pe;
        this.bLayer = b;
    }

    public void send(Message m, List<Host> hosts) {
        for (Host h : hosts) {
            this.perfectLinkLayer.send(h, m.toString());
        }
    }

    public void receive(Host sender, String payload) {
        this.bLayer.receive(sender, new Message(payload));
    }

}
