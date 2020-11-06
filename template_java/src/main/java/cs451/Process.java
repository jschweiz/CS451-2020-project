package cs451;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import cs451.layers.BEBLayer;
import cs451.layers.FIFOLayer;
import cs451.layers.GroundLayer;
import cs451.layers.PerfectLinkLayer;
import cs451.layers.PingLayer;
import cs451.layers.TransportLayer;
import cs451.layers.UBLayer;

public class Process {

    public static boolean RUNNING = true;

    public PerfectLinkLayer perfectLinkLayer;
    private static TransportLayer transportLayer;

    private BEBLayer bebLayer;
    private UBLayer urbLayer;
    private FIFOLayer fifoLayer;

    private static List<String> messageList = Collections.synchronizedList(new LinkedList<>());
    private static int ID = -1;
    private static int PORT = 0;
    private static int CURRENT_BROADCASTED_MESSAGES = 0;
    private static int MAXBROAD = 30000;
    private static int NUMMESSAGES = 10000;
    private static String configPath = null;
    private static String outputPath = null;

    // timer
    private long start = 0;

    public Process(Parser p) {
        ID = p.myId();
        PORT = (int) p.myPort();
        configPath = (p.hasConfig() ? p.config() : null);
        outputPath = p.output();

        transportLayer = new TransportLayer();
        perfectLinkLayer = new PerfectLinkLayer();
        bebLayer = new BEBLayer();
        urbLayer = new UBLayer(ID);
        fifoLayer = new FIFOLayer();

        PingLayer.initializePingLayer(transportLayer, urbLayer, p.hosts(), p.myHost());

        transportLayer.setLayers(perfectLinkLayer);
        perfectLinkLayer.setLayers(transportLayer, bebLayer);
        bebLayer.setLayers(perfectLinkLayer, urbLayer);
        urbLayer.setLayers(bebLayer, fifoLayer);
        fifoLayer.setLayers(urbLayer);

        // initSignalHandlers();
    }

    // Start layers and broadcast messages
    public void broadcastMessages() {
        // start layers
        startLayers();

        // start thread to check number of messages received
        startReporter();

        // start broadcasting
        startBroadcasting(getMessages());
    }

    private void startLayers() {
        start = System.currentTimeMillis();
        GroundLayer.start(PORT);
        PingLayer.start();
    }

    private void startReporter() {
        (new Thread(() -> {
            try {
                Thread.sleep(5000);
                int lastVersion = -1;
                int evolution = 0;
                int numHosts = Host.getHostList().size();
                while (RUNNING) {
                    int messageReceived = messageList.size();
                    if (messageReceived == NUMMESSAGES * (numHosts + 1)) {
                        System.out.println("All " + NUMMESSAGES  + " messages broadcasted in " + (System.currentTimeMillis()-start)/1000 + " s (received " + messageReceived + " messages)" );
                        return;
                        // System.exit(0);
                    } else {
                        System.out.println("Number of messages received= "+ (messageReceived));// - NUMMESSAGES));
                        Thread.sleep(2000);
                    }
                    if (lastVersion == messageReceived) {
                        evolution++;
                        if (evolution == 10) {
                            Main.coord.finishedBroadcasting();
                            break;
                        }
                    } else {
                        lastVersion = messageReceived;
                        evolution = 0;
                    }
                }
            } catch (Exception e) {
                System.err.print(e);
            }
        })).start();
    }

    private void startBroadcasting(List<String> messages) {
        for (String m : messages) {
            while (RUNNING && CURRENT_BROADCASTED_MESSAGES > MAXBROAD) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!RUNNING) return;
            this.fifoLayer.send(m);
            writeInMemory(m, ID, false);
            incrementCurrentBroad(true);
        }
    }

    public static void incrementCurrentBroad(boolean increment) {
        if (increment) CURRENT_BROADCASTED_MESSAGES++;
        else CURRENT_BROADCASTED_MESSAGES--;
    }

    public static void writeInMemory(String message, int pid, boolean delivery) {
        messageList.add((delivery ? "d " + pid + " " : "b ") + message);
        if (delivery && pid == ID) incrementCurrentBroad(false);
    }
    
    private List<String> getMessages() {

        if (configPath != null) {
            File myObj = new File(configPath);
            Scanner myReader;
            try {
                myReader = new Scanner(myObj);
                String data = myReader.nextLine();
                NUMMESSAGES = Integer.parseInt(data);

                if (myReader.hasNext()) {
                    data = myReader.nextLine();
                    MAXBROAD = Integer.valueOf(data);
                }

                myReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        List<String> messagesToBroadcast = new LinkedList<>();
        for (int i = 1; i < NUMMESSAGES + 1; i++) {
            messagesToBroadcast.add("" + i);
        }
        return messagesToBroadcast;
    }
    
    public static void writeInFile() throws IOException {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        RUNNING = false;
        GroundLayer.stop();
        PingLayer.stop();
        transportLayer.stop();

        // writing in output file
        System.out.println("Writing output (" + messageList.size() + " messages received)");
        new PrintWriter(outputPath).close(); // clean the file
        FileWriter globalWriter = new FileWriter(outputPath);
        for (String m : messageList) globalWriter.write(m + "\n");
        globalWriter.close();
    }
}
