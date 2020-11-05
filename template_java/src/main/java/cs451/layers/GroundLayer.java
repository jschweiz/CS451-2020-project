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

	static TransportLayer upLayer;
	static PingLayer pingLayer;

	static PacketList receivedPacketList = new PacketList();

	static Thread thread;
	static DatagramSocket serverSocket = null;

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
		if (serverSocket == null)
			return;
		try {
			serverSocket.send(sendPacket);
		} catch (IOException e) {
			System.err.println("Error while sending in GroundLayer");
		}
	}

	// runs forever in order to receive the packages on a given port
	public static void start(int localPort) {

		// start thread receiving and storing in receivedPacketList List 
		(new Thread(() -> { receivePacketThreadFunction(localPort); })).start();

		// start threads to handle the received
		int numThreads = 5;
		for (int i = 0; i < numThreads; i++) {
			(new Thread(() -> { handleReceivedPacketThreadFunction(); })).start();
		}
	}

	public static void handleReceivedPacketThreadFunction() {
		while(true) {
			upLayer.receive(receivedPacketList.removeFirst());
		}
	}

	public static void receivePacketThreadFunction(int localPort) {
		try {
			System.out.println("Opening socket on port " + localPort);
			serverSocket = new DatagramSocket(localPort);
		} catch (SocketException e) {
			System.err.println("Error during server socket opening on port" + localPort + e.getMessage());
			return;
		}

		DatagramPacket packet = null;
		byte[] buf = new byte[258];

		while (!Thread.currentThread().isInterrupted()) {
			packet = new DatagramPacket(buf, buf.length);
			try {
				serverSocket.receive(packet);
				String data = new String(packet.getData(), 0, packet.getLength()); // to match the length
				if (upLayer != null) {
					Packet p = new Packet(data, packet.getAddress().getHostAddress(), packet.getPort());
					if (p.isPing()) {
						PingLayer.receive(p.destHost, p.destPort);
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
	}
}