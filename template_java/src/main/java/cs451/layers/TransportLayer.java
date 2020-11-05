package cs451.layers;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cs451.utils.Packet;


public class TransportLayer {
    
    private PerfectLinkLayer perfectLinkLayer = null;
    
    private Set<Packet> toBeAcked = Collections.synchronizedSet(new HashSet<Packet>());
    private Set<Packet> received = Collections.synchronizedSet(new HashSet<Packet>());

    private SenderManager manager = new SenderManager();
    private int seqNum = 0;

    public static final int SENDINGPERIOD = 1000;

    // Init function
    public void setLayers(PerfectLinkLayer p) {
        this.perfectLinkLayer = p;
        GroundLayer.deliverTo(this);
    }


    // TaskManager to program sending packets
    class SenderManager {
		private Timer timer;

		public SenderManager() {
			this.timer = new Timer();
		}

		public synchronized void schedule(Packet m) {
			// Define new task
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    if (!toBeAcked.contains(m)) {
                        this.cancel();
                    } else {
                        GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, 0, SENDINGPERIOD);
		}
    }
    

    // Send and receive functions
    public void send(String destHost, int destPort, String payload) {
        Packet m = new Packet(destHost, destPort, payload, seqNum);
        toBeAcked.add(m);
        manager.schedule(m);
        seqNum++;
    }
    
    public void receive(Packet m) {
        String senderName = m.destHost;
        int fromPort = m.destPort;

        if (m.isAck()) { // if message is a ACK
            toBeAcked.remove(m);
        } else { // if normal message
            if (!received.contains(m)) {
                received.add(m);
                perfectLinkLayer.receive(senderName, fromPort, m.payload);
            }
            GroundLayer.send(m.seqNum + ";" + Packet.ACK, senderName, fromPort);
        }
    }


    // Cancel sendings 
    public void cancelSending(String s, int port) {
        LinkedList<Packet> packetToRemove = new LinkedList<Packet>();
        
        synchronized (toBeAcked) {
            for (Packet m : toBeAcked) {
                if ((port == -1 && m.payload.equals(s)) // cancel a specific message
                        || (port != -1 && m.destHost.equals(s)
                        && m.destPort == port)) // cancel messages to a specific host
                {
                    packetToRemove.add(m);
                }
            }
        }
        
        for (Packet m : packetToRemove) {
            toBeAcked.remove(m);
        }
    }
}
