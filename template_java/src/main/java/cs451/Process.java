package cs451;

import java.io.File;
import java.io.FileNotFoundException;
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

    private int id;
    private int port;

    public PerfectLinkLayer perfectLinkLayer;
    private TransportLayer transportLayer;

    private BEBLayer bebLayer;
    private UBLayer urbLayer;
    private FIFOLayer fifoLayer;

    List<String> messageList;
    private int currentBroad;

    public Process(Parser p) {
        id = p.myId();
        port = (int) p.myPort();
        this.messageList = Collections.synchronizedList(new LinkedList<>());
        this.currentBroad = 0;

        transportLayer = new TransportLayer();
        perfectLinkLayer = new PerfectLinkLayer();
        bebLayer = new BEBLayer();
        urbLayer = new UBLayer(id);
        fifoLayer = new FIFOLayer(id);

        PingLayer.initializePingLayer(transportLayer, urbLayer, p.hosts(), p.myHost());

        transportLayer.setLayers(perfectLinkLayer);
        perfectLinkLayer.setLayers(transportLayer, bebLayer, this);
        bebLayer.setLayers(perfectLinkLayer, urbLayer, this);
        urbLayer.setLayers(bebLayer, fifoLayer, this);
        fifoLayer.setLayers(urbLayer, this);

    }

    private List<String> getMessages() {
        List<String> messagesToBroadcast = new LinkedList<>();
        for (int i = 0; i < 50; i++) {
            messagesToBroadcast.add("" + i + ":" + this.id);
        }
        return messagesToBroadcast;
    }

    // Start layers and broadcast messages
    public void broadcastMessages() {

        int MAXBROAD = 30000;

        // start layers
        startLayers();

        // start thread to check number of messages received
        (new Thread(() -> {
            try {
                writeReceivedMessage();
            } catch (Exception e) {
                System.err.print(e);
            }
        })).start();

        // if (id == 1) {
        // broadcast from all
        for (String m : getMessages()) {
            while (currentBroad > MAXBROAD) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.fifoLayer.send(m);
            currentBroad++;
        }
        // }
    }

    private void startLayers() {
        GroundLayer.start(this.port);
        PingLayer.start();
    }

    private void writeReceivedMessage() throws IOException {
        int rec = 0;
        String path = "outputs/message_received_" + this.id + ".log";
        new PrintWriter(path).close();
        FileWriter myWriter[] = new FileWriter[3];
        for (int i = 1; i < 4; i++) {
            path = "outputs/message_received_" + this.id + "_from_"+ i  +".log";
            myWriter[i-1] = new FileWriter(path);
        }
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            synchronized (messageList) {
                rec += this.messageList.size();
                System.out.println("Number of messages received= "+ rec +"");
                for (String m : this.messageList) {
                    int num = Integer.parseInt(m.split(":")[1]);
                    myWriter[num-1].write(m+"\n");
                }
                for (int i = 0; i < 3; i++) {
                    myWriter[i].flush();
                }
               
                messageList.clear();
            }
        }
    }


    // Reading and writing to files
    public static List<String> readFromFile(String path) {
        LinkedList<String> list = new LinkedList<>();
        try {
            File file = new File(path);
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                list.add(scan.nextLine());
            }
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }


    public void writeInFile(String message, int id) {
        this.messageList.add(message);
        if (id == this.id) {
            currentBroad--;
        }
    }
}
