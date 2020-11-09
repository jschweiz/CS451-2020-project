package cs451.layers;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cs451.Host;
import cs451.utils.Packet;
import cs451.utils.ConcurrencyManager;


public class PingLayer {
    
    private static final int DELAY = ConcurrencyManager.SENDING_PERIOD_PING;
    private static final int PERIOD = ConcurrencyManager.CHECKING_PERIOD_PING;

    // logging purposes
    private static final Logger LOG = Logger.getLogger("TransportLayer");
    private static final String ORIGIN_STRING = "[PING] ";
    private static final String LOST_STRING = ORIGIN_STRING + "NO ANSWER FROM %d;%d";

    // layers
    private static TransportLayer transportLayer; // to send pings
    private static UBLayer bLayer;

    // variables
    private static List<Host> listAllGoodHosts;
    private static Set<Packet> receivedPings;

    // manager and thread stopper
    private static PingSenderManager manager = new PingSenderManager();
    private static boolean RUNNING = true;


    // intialize layer
    public static void initializePingLayer(TransportLayer tl, UBLayer r, List<Host> hosts, Host ownHost) {
        listAllGoodHosts = Collections.synchronizedList(new LinkedList<Host>());
        listAllGoodHosts.addAll(hosts);
        listAllGoodHosts.remove(ownHost);
        receivedPings = Collections.synchronizedSet(new HashSet<Packet>());
        transportLayer = tl;
        bLayer = r;
    }

    public static void start() {
        manager.schedulePingSending(); // sending periodically pings
        manager.scheduleCheck(); // checking periodically if all ping received
    }

    public static void stop() {
        RUNNING = false;
        listAllGoodHosts.clear();
    }


    // function for checking pings

    private static List<Host> removeLostHosts() {
        List<Host> crashedHost = new LinkedList<>();

        
        StringBuilder activeHostsString = new StringBuilder();
        activeHostsString.append(ORIGIN_STRING + "Active hosts:  ");
        // Check if ping have been received from all hosts
        synchronized (listAllGoodHosts) {
            listAllGoodHosts.forEach((e) -> {
                activeHostsString.append(e.getIp() + ";" + e.getPort() + "  ");
                if (!receivedPings.contains(new Packet(e.getIp(), e.getPort(), Packet.PING, Packet.SEQ_NUM_PING))) {
                    crashedHost.add(e);
                }
            });
        }
        LOG.info(activeHostsString.toString());
        
        // remove the one for which no ping have been received
        for (Host cHost : crashedHost) {
            listAllGoodHosts.remove(cHost);
            LOG.info(String.format(LOST_STRING, cHost.getId(), cHost.getPort()));
        }
        receivedPings.clear(); // reset the table of received pings
        return crashedHost;
    }


    // update the table of recevied pings
    public static void receive(Packet p) {
        receivedPings.add(p);
    }

    // Getters 
    public static List<Host> currentAvailableHosts() {
        List<Host> copy = new LinkedList<>();
        synchronized(listAllGoodHosts) {
            copy.addAll(listAllGoodHosts);
        }
        return copy;
    }


    private static class PingSenderManager {
		private Timer timer = new Timer();

		public synchronized void schedulePingSending() {
			// Define new task
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    synchronized (listAllGoodHosts) {
                        listAllGoodHosts.forEach((e) -> {
                            Packet m = new Packet(e.getIp(), e.getPort(), Packet.PING, Packet.SEQ_NUM_PING);
                            GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
                        });
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, 0, DELAY);
        }
        
        public synchronized void scheduleCheck() {
            TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    if (!RUNNING) this.cancel();

                    List<Host> crashedHosts = removeLostHosts();
    
                    if (!crashedHosts.isEmpty()) {
                        for (Host h : crashedHosts) {
                            transportLayer.cancelSending(h.getIp(), h.getPort());
                        }
                        bLayer.lostConnectionTo(listAllGoodHosts);
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, PERIOD, PERIOD);
        }
    }
}
