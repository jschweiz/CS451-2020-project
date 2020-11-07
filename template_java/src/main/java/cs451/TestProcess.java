package cs451;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import cs451.layers.GroundLayer;
import cs451.layers.PerfectLinkLayer;
import cs451.layers.PingLayer;
import cs451.layers.TransportLayer;
import cs451.utils.ConcurrencyManager;

public class TestProcess {

    public static boolean RUNNING = true;

    public PerfectLinkLayer perfectLinkLayer;
    private static TransportLayer transportLayer;

    private static List<String> messageList = Collections.synchronizedList(new LinkedList<>());
    private static int ID = -1;
    private static int PORT = 0;
    private static int NUMMESSAGES = 10000;
    private static String configPath = null;

    public static TestProcess currProcess;

    public static boolean loggedTiming = false;

    // timer
    private long start = 0;

    public TestProcess(Parser p) {
        ID = p.myId();
        PORT = (int) p.myPort();
        configPath = (p.hasConfig() ? p.config() : null);

        transportLayer = new TransportLayer();
        perfectLinkLayer = new PerfectLinkLayer(ID);

        PingLayer.initializePingLayer(transportLayer, null, p.hosts(), p.myHost());

        transportLayer.setLayers(perfectLinkLayer);
        perfectLinkLayer.setLayers(transportLayer, null);

        currProcess = this;
    }

    // Start layers and broadcast messages
    public void broadcastMessages() {
        // start layers
        startLayers();

        // start thread to check number of messages received
        (new ReportManager()).schedule();

        // start broadcasting
        startBroadcasting(getMessages());
    }

    private void startLayers() {
        start = System.currentTimeMillis();
        GroundLayer.start(PORT);
        PingLayer.start();
    }

     // TaskManager to program sending packets
     private class ReportManager {
        private Timer timer;
        private long number;

        private long old_version;
        private int repeat;

		public ReportManager() {
            this.timer = new Timer();
            this.number = 0;
		}

		public synchronized void schedule() {
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    synchronized (messageList) {
                        long messageReceived = messageList.size() + number;
                   
                        if (ID == 1) {
                            System.out.println("Number of messages received= " + (messageReceived));
                        } else {
                            System.out.println("Number of messages sent= " + (messageReceived));
                        }
    
                        if (messageList.size() > 1000000) {
                            System.out.println("Cleaning final map");
                                number = messageReceived;
                                messageList.clear();
                        }
    
                        if (messageReceived == old_version) {
                            repeat++;
                            if (repeat == 10) {
                                ConcurrencyManager.DEBUG = true;
                                System.out.println("LOGGING DEBUG");
                            }
                        } else {
                            repeat = 0;
                        }
                        old_version = messageReceived;

                        if (!loggedTiming && messageReceived >= 5000000) {
                            System.out.println("Timing : " + (System.currentTimeMillis() - (double)start)/1000 + " s");
                            loggedTiming = true;
                        }
                    }
				}
            };
            int delay = ConcurrencyManager.CHECKING_PERIOD_MESSAGE_RECEIVED;
            this.timer.scheduleAtFixedRate(task, delay, delay);
        }
    }

    private void startBroadcasting(int numMessages) {
        for (int i = 0; i < numMessages; i++) {
            if (!RUNNING) return;
            // int friend = (ID == 1) ? 1 : 0;

            String m = "" + i;

            if (ID == 1) {
                this.perfectLinkLayer.send(Host.getHostList().get(1), m);
                writeInMemory(m, ID, false);
            }
        }
    }


    public void writeInMemory(String message, int pid, boolean delivery) {
        messageList.add((delivery ? "d " + pid + " " : "b ") + message);
    }
    
    private int getMessages() {

        if (configPath != null) {
            File myObj = new File(configPath);
            Scanner myReader;
            try {
                myReader = new Scanner(myObj);
                String data = myReader.nextLine();
                NUMMESSAGES = Integer.parseInt(data);

                myReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return NUMMESSAGES;
    }
}
