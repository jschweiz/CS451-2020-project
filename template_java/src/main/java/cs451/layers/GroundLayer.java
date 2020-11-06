package cs451.layers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import cs451.utils.Packet;
import cs451.utils.PacketList;

public class GroundLayer {

	private static boolean RUNNING = true;

	private static TransportLayer upLayer;

	private static PacketList receivedPacketList = new PacketList();
	private static DatagramSocket serverSocket = null;

	public static void deliverTo(TransportLayer layer) {
		upLayer = layer;
	}

	// called by above layer to send packet
	public static void send(String payload, String destinationHostName, int destPort) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(destinationHostName);
		} catch (UnknownHostException e) {
			System.err.println("Error" + e.getMessage());
		}

		byte[] buf = payload.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, destPort);

		if (!RUNNING || serverSocket == null) return;
		try {
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			System.err.println("Error while sending in GroundLayer");
		}
	}

	// start 2 types of threads
	public static void start(int localPort) {
		// start thread receiving and storing in receivedPacketList List 
		(new Thread(() -> receivePacketThreadFunction(localPort))).start();

		// start threads to process the received packets
		int numThreads = 2;
		for (int i = 0; i < numThreads; i++) {
			(new Thread(() -> handleReceivedPacketThreadFunction())).start();
		}
	}

	public static void stop() {
		RUNNING = false;
	}

	// processing received packet threads : numThreads
	private static void handleReceivedPacketThreadFunction() {
		while(RUNNING) {
			upLayer.receive(receivedPacketList.removeFirst());
		}
	}

	// receiver packet thread : 1
	private static void receivePacketThreadFunction(int localPort) {
		try {
			System.out.println("Opening socket on port " + localPort);
			serverSocket = new DatagramSocket(localPort);
		} catch (SocketException e) {
			System.err.println("Error during server socket opening on port" + localPort + e.getMessage());
			return;
		}

		DatagramPacket packet = null;
		byte[] buf = new byte[258];

		while (!Thread.currentThread().isInterrupted() && RUNNING) {
			packet = new DatagramPacket(buf, buf.length);
			try {
				serverSocket.receive(packet);
				String data = new String(packet.getData(), 0, packet.getLength()); // to match the length
				if (upLayer != null) {
					Packet p = new Packet(data, packet.getAddress().getHostAddress(), packet.getPort());

					// handle ping here so they are not stuck behind queue
					if (p.isPing()) {
						PingLayer.receive(p);
					} else {
						receivedPacketList.add(p);
					}
				}
			} catch (SocketException e) {
				System.err.println("-- socket closed --");
				Thread.currentThread().interrupt();
			} catch (IOException e) {
				System.err.println(e);
				Thread.currentThread().interrupt();
			}
		}

		serverSocket.close();
	}
}