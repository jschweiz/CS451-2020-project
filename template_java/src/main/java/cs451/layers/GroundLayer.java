package cs451.layers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.utils.ConcurrencyManager;
import cs451.utils.Packet;

public class GroundLayer {

	private static boolean RUNNING = true;
	public static final boolean optimizedThreads = ConcurrencyManager.REDUCE_THREADS;
	public static final int numThreads = ConcurrencyManager.NUM_THREAD_PACKET_HANDLER;

	private static TransportLayer upLayer;

	private static LinkedBlockingQueue<Packet> receivedPacketList = new LinkedBlockingQueue<Packet>();
	private static DatagramSocket serverSocket = null;

	public static void deliverTo(TransportLayer layer) {
		upLayer = layer;
	}

	// called by above layer to send packet
	public static void send(String payload, String destinationHostName, int destPort) {
		if (!RUNNING || serverSocket == null) return;

		InetAddress address = null;
		try {
			address = InetAddress.getByName(destinationHostName);
		} catch (IOException e) {
			System.err.println("Error while sending in GroundLayer");
		}

		byte[] buf = payload.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, destPort);

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
		if (!optimizedThreads) {
			for (int i = 0; i < numThreads; i++) {
				(new Thread(() -> handleReceivedPacketThreadFunction())).start();
			}
		}
	}

	public static void stop() {
		RUNNING = false;
	}

	// processing received packet threads : numThreads
	private static void handleReceivedPacketThreadFunction() {
		while (RUNNING) {
			try {
				Packet m = null;
				synchronized (receivedPacketList) {
					m = receivedPacketList.take();
				}
				upLayer.receive(m);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
				Packet p = new Packet(
						new String(packet.getData(), 0, packet.getLength()),
						packet.getAddress().getHostAddress(), packet.getPort());

				// handle ping here so they are not stuck behind queue
				if (p.isPing()) {
					PingLayer.receive(p);
				} else  if (optimizedThreads) {
					upLayer.receive(p);
				} else {
					receivedPacketList.add(p);
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