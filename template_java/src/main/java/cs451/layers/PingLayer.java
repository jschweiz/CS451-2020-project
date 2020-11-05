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


public class PingLayer {

    private static TransportLayer transportLayer; // to send pings
    private static UBLayer bLayer;

    private static List<Host> listAllGoodHosts;
    private static Set<Host> currentHosts;

    static final int DELAY = 500;
    static final int PERIOD = 10000;

    private static PingSenderManager manager = new PingSenderManager();

    public static class PingSenderManager {
		private Timer timer;

		public PingSenderManager() {
			this.timer = new Timer();
		}

		public synchronized void schedule(Packet m) {
			// Define new task
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    Host h = Host.findHost(m.destHost, m.destPort);
                    if (!listAllGoodHosts.contains(h)) {
                        this.cancel();
                    } else {
                        GroundLayer.send(m.encapsulate(), m.destHost, m.destPort);
                    }
				}
			};
			this.timer.scheduleAtFixedRate(task, 0, DELAY);
		}
    }

    // intialize layer
    public static void initializePingLayer(TransportLayer tl, UBLayer r, List<Host> hosts, Host ownHost) {
        listAllGoodHosts = Collections.synchronizedList(new LinkedList<Host>());
        for (Host host: hosts) {
            if (!host.equals(ownHost)) {
                listAllGoodHosts.add(host);
            }
        }
        currentHosts = Collections.synchronizedSet(new HashSet<Host>());
        transportLayer = tl;
        bLayer = r;
    }


    // start ping layer
    public static void start() {
        // program the sending of pings
        programPingSending();
        // start checking if all ping are received
        createCheckThread().start();
    }


    // program sending the pings
    private static void programPingSending() {
        synchronized (listAllGoodHosts) {
            for (Host host : listAllGoodHosts) {
                manager.schedule(new Packet(host.getIp(), host.getPort(),
                        Packet.PING,0));
            }
        }
    }


    // check if ping received from all good hosts
    private static Thread createCheckThread() {
        return new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(PERIOD);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                checkStillHaveHosts();
                List<Host> crashedHosts = removeLostHosts();

                if (!crashedHosts.isEmpty()) {
                    cancelSendingMessageInVoid(crashedHosts);
                    bLayer.lostConnectionTo(listAllGoodHosts);
                }
            }
        });
    }

    private static void checkStillHaveHosts() {
        if (listAllGoodHosts.isEmpty()) {
            System.out.println("NO FRIENDS TO PING :/ (STOPPED)");
            Thread.currentThread().stop();
        }
        System.out.print("Checking if all hosts are still alive (current list: ");
        synchronized (listAllGoodHosts) {
            for (Host h: listAllGoodHosts) {
                System.out.print(h.getIp() + ";" + h.getPort() + "  ");
            }
        }
        System.out.println(")");
    }

    private static List<Host> removeLostHosts() {
        List<Host> crashedHost = new LinkedList<Host>();
        // Check if ping have been received from all hosts
        synchronized (listAllGoodHosts) {
            for (Host host : listAllGoodHosts) {
                if (!currentHosts.contains(host)) {
                    crashedHost.add(host);
                }
            }
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
    public static void receive(String senderName, int fromPort) {
        currentHosts.add(Host.findHost(senderName, fromPort));
    }


    // Getters 
    public static List<Host> currentAvailableHosts() {
        return listAllGoodHosts;
    }


}
