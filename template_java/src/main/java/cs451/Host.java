package cs451;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Host {

    private static final String IP_START_REGEX = "/";

    private int id;
    private String ip;
    private int port = -1;

    public Host(int id, String ip, int p) {
        this.id = id;
        this.ip = ip;
        this.port = p;
    }

    public Host() {}

    public synchronized boolean populate(String idString, String ipString, String portString) {
        try {
            id = Integer.parseInt(idString);

            String ipTest = InetAddress.getByName(ipString).toString();
            if (ipTest.startsWith(IP_START_REGEX)) {
                ip = ipTest.substring(1);
            } else {
                ip = InetAddress.getByName(ipTest.split(IP_START_REGEX)[0]).getHostAddress();
            }

            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts file must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts file must be a number!");
            } else {
                System.err.println("Port in the hosts file must be a number!");
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized int getId() {
        return id;
    }

    public synchronized String getIp() {
        return ip;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized boolean equals(Object o) {
        if (!(o instanceof Host)) return false;
        Host m = (Host) o;
        return ip.equals(m.ip) && id == m.id && port == m.port;
    }

    public synchronized int hashCode() {
        return id + ip.hashCode() + port;
    }

    public synchronized String toString() {
        return "(" + this.id + ";" + this.ip + ";" + this.port + ")";
    }


    // static functions added
    private static List<Host> hostList = Collections.synchronizedList(new LinkedList<>());
    
    public static synchronized void setHostList(List<Host> l) {
        hostList.addAll(l);
    }

    public static synchronized List<Host> getHostList() {
        return hostList;
    }

    public static synchronized Host findHost(String ip, int port) {
        for (Host h: hostList) {
            if (h.getIp().equals(ip) && h.getPort() == port) {
                return h;
            }
        }
        return null;
    }
}
