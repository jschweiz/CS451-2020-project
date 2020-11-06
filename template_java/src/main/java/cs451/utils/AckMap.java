package cs451.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cs451.Host;

public class AckMap {

    private HashMap<Message, Set<Host>> map;

    public AckMap() {
        this.map = new HashMap<>();
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

    public synchronized Set<Message> getAllMessages() {
        Set<Message> s = new HashSet<>();
        s.addAll( map.keySet());
        return s;
    }
}
