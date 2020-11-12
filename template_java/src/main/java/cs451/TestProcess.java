package cs451;

import java.util.logging.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import cs451.layers.BEBLayer;
import cs451.layers.FIFOLayer;
import cs451.layers.GroundLayer;
import cs451.layers.PerfectLinkLayer;
import cs451.layers.PingLayer;
import cs451.layers.TransportLayer;
import cs451.layers.UBLayer;
import cs451.utils.ConcurrencyManager;

public class TestProcess {

    private static final long MAX_LIST_SIZE = 1000000;
    private static final long NUM_TO_TIME = 500000;
    private static final int DELAY = ConcurrencyManager.CHECKING_PERIOD_MESSAGE_RECEIVED;
    private static final boolean WRITE_TO_FILE = false;

    // logging purposes
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT] %5$s %n");
    }
    private static Logger LOG = Logger.getLogger(TestProcess.class.getName());
    private static final String ORIGIN_STRING = "[PROCESS] ";
    private static final String N_STRING = ORIGIN_STRING + "Number of messages received: %d";
    private static final String GC_STRING = ORIGIN_STRING + "Cleaned final string list (over %d elements) (time: %f)";
    private static final String TIMING_STRING = ORIGIN_STRING + "Timing : %f s to broadcast %d messages";

    // himself as main TestProcess
    public static TestProcess CURRENT_PROCESS;

    // layers
    private PerfectLinkLayer perfectLinkLayer;
    private TransportLayer transportLayer;
    private BEBLayer bebLayer;
    private UBLayer ubLayer;
    private FIFOLayer fifoLayer;

    // variables
    private List<String> messageList = Collections.synchronizedList(new ArrayList<>(2000000));
    private boolean loggedTiming = false;
    private long numMessagesToBroadcast = 1000;
    private long selfDelivered = 0;
    private final Object finishedLock = new Object();

    private final int id;
    private final int port;
    private final String configPath;
    private final String outputPath;

    // reportmanager and thread stopper
    private ReportManager manager = new ReportManager();
    public boolean RUNNING = true;

    // timing
    private long start = 0;

    public TestProcess(Parser p) {
        this.id = p.myId();
        this.port = (int) p.myPort();
        this.configPath = (p.hasConfig() ? p.config() : null);
        this.outputPath = p.output();
        this.numMessagesToBroadcast = readConfigFile();

        // initialize layers
        transportLayer = new TransportLayer();
        perfectLinkLayer = new PerfectLinkLayer(id);
        bebLayer = new BEBLayer();
        ubLayer = new UBLayer(id, p.hosts().size());
        fifoLayer = new FIFOLayer();

        // link static layers
        PingLayer.initializePingLayer(transportLayer, ubLayer, p.hosts(), p.myHost());
        GroundLayer.deliverTo(transportLayer);

        // link other layers
        transportLayer.setLayers(perfectLinkLayer);
        perfectLinkLayer.setLayers(transportLayer, bebLayer);
        bebLayer.setLayers(perfectLinkLayer, ubLayer);
        ubLayer.setLayers(bebLayer, fifoLayer);
        fifoLayer.setLayers(ubLayer);

        // set this as the main process
        CURRENT_PROCESS = this;
    }

    // Start layers and broadcast messages
    public void broadcastMessages() {
        // start layers
        startLayers();

        // start thread to check number of messages received
        manager.schedule();

        // start broadcasting
        startBroadcasting(numMessagesToBroadcast);

        synchronized (finishedLock) {
            try {
                finishedLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOG.info("[PROCESS] ======================================================================");
        LOG.info("[PROCESS] Broadcasted all " + this.selfDelivered + " messages to himself");
        LOG.info("[PROCESS] ======================================================================");
    }

    private void startLayers() {
        start = System.currentTimeMillis();
        GroundLayer.start(this.port);
        PingLayer.start();
    }

    private void startBroadcasting(long numMessages) {
        Thread.currentThread().setName("CUSTOM_main_sender_thread");
        for (long i = 1; i < numMessages + 1; i++) {
            if (!RUNNING)
                return;
                String m = Long.toString(i);
            fifoLayer.send(m);
            writeInMemory(m, -1, false);
        }
    }

    // function called when a message has been received
    public void writeInMemory(String message, int pid, boolean delivery) {
        synchronized (messageList) {
            messageList.add((delivery ? "d " + pid + " " : "b ") + message);
            if (pid == this.id) {
                this.selfDelivered++;
            }
            if (this.selfDelivered == this.numMessagesToBroadcast) {
                synchronized (finishedLock) {
                    finishedLock.notifyAll();
                }
            }
        }
    }

    public void writeInFile() {
        System.out.println("Stopped. Writing to file after running for " + (System.currentTimeMillis()/1000.0-this.start/1000.0) + " s.");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(this.outputPath));

            synchronized(messageList) {
                messageList.forEach((e) -> {
                    try {
                        writer.write(e+"\n");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                });
            }

            writer.close();
            
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    // read number of messages to send
    private long readConfigFile() {
        long numMessages = this.numMessagesToBroadcast;
        if (configPath != null) {
            File myObj = new File(configPath);
            Scanner myReader;
            try {
                myReader = new Scanner(myObj);
                numMessages = Long.parseLong(myReader.nextLine());

                myReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return numMessages;
    }


    // TaskManager to program sending packets
    private class ReportManager {
        private Timer timer = new Timer();
        private long archivedMessagesNumber = 0;

		public void schedule() {
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
                    Thread.currentThread().setName("CUSTOM_gc_process_timer");
                    synchronized (messageList) {
                        long currListSize = messageList.size();
                        long messageReceived = currListSize + archivedMessagesNumber;

                        LOG.info(String.format(N_STRING, messageReceived));
    
                        if (currListSize >= MAX_LIST_SIZE) {
                            archivedMessagesNumber = messageReceived;
                            if (WRITE_TO_FILE) {
                                writeInFile();
                            }
                            messageList.clear();
                            LOG.info(String.format(GC_STRING, MAX_LIST_SIZE, (System.currentTimeMillis() - start)/1000.0, NUM_TO_TIME));
                        }

                        if (!loggedTiming && messageReceived >= NUM_TO_TIME) {
                            LOG.info(String.format(TIMING_STRING, (System.currentTimeMillis() - start)/1000.0, NUM_TO_TIME));
                            loggedTiming = true;
                        }
                    }
				}
            };
            this.timer.scheduleAtFixedRate(task, DELAY, DELAY);
        }
    }
}
