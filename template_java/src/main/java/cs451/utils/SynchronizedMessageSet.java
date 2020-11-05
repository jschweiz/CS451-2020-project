package cs451.utils;

import java.util.HashSet;
import java.util.Set;

public class SynchronizedMessageSet {

    private Set<Message> set;

    private static int MAXSIZE = 1000000;

    public SynchronizedMessageSet() {
        set = new HashSet<>();
    }

    public synchronized void add(Message m) {
        // while (set.size() > MAXSIZE) {
        //     try {
        //         wait();
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }
        set.add(m);
    }

    public synchronized void addForced(Message m) {
        set.add(m);
    }

    public synchronized void remove(Message m) {
        set.remove(m);
        // notifyAll();
    }

    public synchronized boolean contains(Message m) {
        return set.contains(m);
    }

}