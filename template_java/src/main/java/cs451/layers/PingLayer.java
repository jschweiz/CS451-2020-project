package cs451.layers;

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

    private static boolean RUNNING = true;

    private static TransportLayer transportLayer; // to send pings
    private static UBLayer bLayer;

    private static List<Host> listAllGoodHosts;
    private static Set<Host> currentHosts;

    static final int DELAY = ConcurrencyManager.SENDING_PERIOD_PING;
    static final int PERIOD = ConcurrencyManager.CHECKING_PERIOD_PING;

    private static PingSenderManager manager = new PingSenderManager();

    private static class PingSenderManager {
		private Timer timer;

		public PingSenderManager() {
			this.timer = new Timer();
		}

		public synchronized void schedule(Packet m) {
			// Define new task
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    Host h = m.getHost();
                    if (!listAllGoodHosts.contains(h)) {
                        this.cancel();
                    } else {
                        GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, 0, DELAY);
        }
        
        public synchronized void schedule() {
            TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    if (!RUNNING) this.cancel();

                    checkStillHaveHosts();
                    List<Host> crashedHosts = removeLostHosts();
    
                    if (!crashedHosts.isEmpty()) {
                        cancelSendingMessageInVoid(crashedHosts);
                        bLayer.lostConnectionTo(listAllGoodHosts);
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, PERIOD, PERIOD);
        }
    }

    // intialize layer
    public static void initializePingLayer(TransportLayer tl, UBLayer r, List<Host> hosts, Host ownHost) {
        listAllGoodHosts = Collections.synchronizedList(new LinkedList<Host>());
        listAllGoodHosts.addAll(hosts);
        listAllGoodHosts.remove(ownHost);
        currentHosts = Collections.synchronizedSet(new HashSet<Host>());
        transportLayer = tl;
        bLayer = r;
    }


    // start and stop ping layer
    public static void start() {
        // program the sending of pings
        programPingSending();
        // start checking if all ping are received
        manager.schedule();
    }

    public static void stop() {
        RUNNING = false;
        synchronized (listAllGoodHosts) {
            listAllGoodHosts.clear();
        }
    }


    // program sending the pings
    private static void programPingSending() {
        synchronized (listAllGoodHosts) {
            for (Host host : listAllGoodHosts) {
                manager.schedule(new Packet(host.getIp(), host.getPort(), Packet.PING,0));
            }
        }
    }

    private static void checkStillHaveHosts() {
        if (listAllGoodHosts.isEmpty()) {
            System.out.println("NO FRIENDS TO PING :/ (STOPPED)");
            stop();
        }
        StringBuilder activeHostsString = new StringBuilder();
        activeHostsString.append("Active hosts:  ");
        synchronized (listAllGoodHosts) {
            for (Host h: listAllGoodHosts) {
                activeHostsString.append(h.getIp() + ";" + h.getPort() + "  ");
            }
        }
        System.out.println(activeHostsString);
    }

    private static List<Host> removeLostHosts() {
        List<Host> crashedHost = new LinkedList<>();
        // Check if ping have been received from all hosts
        synchronized (listAllGoodHosts) {
            listAllGoodHosts.forEach((e) -> {
                if (!currentHosts.contains(e)) {
                    crashedHost.add(e);
                }
            });
        }
        
        // remove the one for which no ping have been received
        for (Host cHost : crashedHost) {
            listAllGoodHosts.remove(cHost);
            System.out.println("NO ANSWER FROM " + cHost.getId() + ";" + cHost.getPort());
        }
        currentHosts.clear(); // reset the table of received pings
        return crashedHost;
    }
    
    private static void cancelSendingMessageInVoid(List<Host> crashedHosts) {
        for (Host h : crashedHosts) {
            transportLayer.cancelSending(h.getIp(), h.getPort());
        }
    }

    // update the table of recevied pings
    public static void receive(Packet p) {
        currentHosts.add(p.getHost());
    }

    // Getters 
    public static List<Host> currentAvailableHosts() {
        return listAllGoodHosts;
    }
}
