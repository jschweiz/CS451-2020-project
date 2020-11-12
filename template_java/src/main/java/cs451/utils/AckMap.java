package cs451.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import cs451.Host;

public class AckMap {

    private HashMap<Message, Set<Host>> map;

    public AckMap() {
        this.map = new HashMap<>(1000000); // reduced because of some segv error (number 2564) 
        // dvided size for small numbers by 10
    }

    public synchronized void ackBy(Message m, Host h) {
        if (!map.containsKey(m)) {
            map.put(m, new HashSet<>());
        }
        map.get(m).add(h);
    }

    public synchronized boolean allAcked(Message m, List<Host> hosts) {
        if (!map.containsKey(m)) {
            return false;
        }
        Set<Host> s = map.get(m);
        for (Host h : hosts) {
            if (!s.contains(h))
                return false;
        }
        map.remove(m);
        return true;
    }

    public synchronized List<Message> getAlreadyAckedMessage(List<Host> hosts) {
        List<Message> messageList = new LinkedList<>();

        Set<Message> copySet = new HashSet<>(this.map.keySet());

        for (Message m : copySet) {
            if (allAcked(m, hosts)) {
                messageList.add(m);
            }
        }
        System.out.println("Removed " + messageList.size() + " messages to deliver");
        return messageList;
    }

    public synchronized int size() {
        return map.size();
    }
}
