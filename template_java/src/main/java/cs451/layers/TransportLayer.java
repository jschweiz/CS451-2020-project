package cs451.layers;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cs451.utils.Packet;
import cs451.utils.ConcurrencyManager;


public class TransportLayer {
    
    private PerfectLinkLayer perfectLinkLayer = null;
    
    private Set<Packet> toBeAcked = Collections.synchronizedSet(new HashSet<Packet>());
    private Set<Packet> received = Collections.synchronizedSet(new HashSet<Packet>());

    private SenderManager manager = new SenderManager();
    private long seqNum = 0;

    private static final int SENDINGPERIOD = ConcurrencyManager.SENDING_PERIOD_PACKET;

    // Init function
    public void setLayers(PerfectLinkLayer p) {
        this.perfectLinkLayer = p;
        GroundLayer.deliverTo(this);
    }

    public void stop() {
        synchronized(toBeAcked) {
            toBeAcked.clear();
        }
    }

    // TaskManager to program sending packets
    private class SenderManager {
        private Timer timer;

		public SenderManager() {
			this.timer = new Timer();
		}

		public synchronized void schedule(Packet m) {
			TimerTask task = new TimerTask() {
				@Override
				public synchronized void run() {
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
    

    public void send(String destHost, int destPort, String payload) {
        Packet m = new Packet(destHost, destPort, payload, seqNum);
        toBeAcked.add(m);
        manager.schedule(m);
        seqNum++;
    }
    
    public synchronized void receive(Packet m) {
        if (m.isAck()) { // if message is a ACK
            toBeAcked.remove(m);
        } else { // if normal message
            if (!received.contains(m)) {
                received.add(m);
                deliver(m);
            }
            GroundLayer.send(m.toAckPacket(), m.destHost, m.destPort);
        }
    }

    public void deliver(Packet m) {
        perfectLinkLayer.receive(m.destHost, m.destPort, m.payload);
    }

    // Cancel sendings 
    public synchronized void cancelSending(String ip, int port) {
        LinkedList<Packet> packetToRemove = new LinkedList<>();
        synchronized (toBeAcked) { // make sure lock is kept when iteration on toBeAcked
            for (Packet m : toBeAcked) {
                if (port != -1 && m.destHost.equals(ip)
                        && m.destPort == port) // cancel messages to a specific host
                {
                    packetToRemove.add(m);
                }
            }
            toBeAcked.removeAll(packetToRemove);
        }
    }
}
