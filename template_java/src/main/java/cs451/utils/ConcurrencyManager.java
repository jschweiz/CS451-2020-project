package cs451.utils;

public class ConcurrencyManager {

    // handle congestion at the ub layer
    public static final int MAX_CONCURRENT_MESSAGES = 100000;

    // number of thread working :) 
    public static final boolean REDUCE_THREADS = false;
    public static final int NUM_THREAD_PACKET_HANDLER = 1;


    // sets the time before resending a message if not ack
    public static final int SENDING_PERIOD_PACKET = 1000;
    public static final int SENDING_PERIOD_PING = 400;


    // recurrent function to execute
    public static final int CHECKING_PERIOD_PING = 5000; // check the lost processes
    public static final int CHECKING_PERIOD_MESSAGE_RECEIVED = 2000; // check toal number of messages received


    public static boolean DEBUG = false;


    // garbage collection of transport layer
    public static final int NBINS = 15;
    public static final int GARBAGECOLLECTIONPERIOD = SENDING_PERIOD_PACKET * 8;
    public static final int MAXNUMBEROFCONCURRENTPACKETSPERBIN = 1000;


    // parameters:
    // 1 sender: 1000 30 7 5000
    // 2 sender:

}