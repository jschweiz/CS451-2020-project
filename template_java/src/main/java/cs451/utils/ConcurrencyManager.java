package cs451.utils;

public class ConcurrencyManager {

    // handle congestion at the ub layer
    public static final int MAX_CONCURRENT_MESSAGES = 15000;

    public static final int getUBMaxConcurrentMessages(int numProcess) {
        // if (numProcess == 1) return MAX_CONCURRENT_MESSAGES;
        // return MAX_CONCURRENT_MESSAGES / (numProcess * numProcess * (numProcess - 1));
        return 30;
    }

    // number of thread working :) 
    public static final boolean REDUCE_THREADS = false;
    public static final int NUM_THREAD_PACKET_HANDLER = 1;


    // sets the time before resending a message if not ack
    public static final int SENDING_PERIOD_PACKET = 700;
    public static final int SENDING_PERIOD_PING = 100;


    // recurrent function to execute
    public static final int CHECKING_PERIOD_PING = 10000; // check the lost processes
    public static final int CHECKING_PERIOD_MESSAGE_RECEIVED = 2000; // check toal number of messages received


    public static boolean DEBUG = false;


    // garbage collection of transport layer
    public static final int CONCURRENT_UDP_PACKETS = 200000;
    public static final int MAXNUMBEROFCONCURRENTPACKETSPERBIN = 1000;
    public static final int NBINS = CONCURRENT_UDP_PACKETS / (MAXNUMBEROFCONCURRENTPACKETSPERBIN * 10);
    static {
        System.out.println("nbins = "+ NBINS);
    }
    public static final int GARBAGECOLLECTIONPERIOD = SENDING_PERIOD_PACKET * 15;



    // garbage collections
    public static final boolean GC_UB_ENABLED = false;
    public static final boolean GC_TR_ENABLED = false;


    // parameters:
    // 1 sender: 1000 30 7 5000
    // 2 sender:



    // test of speed:
    /*
        1000        22.5        slow
        1000        9           very slow
        1000        2.25        still slow
        500         22.5        84
        500         9           82
        500         2.25        81
        200         22.5        82
        200         9           80
        200         2.25        79

        200         22.5        82          100

        200         3                       800      500        80
        200         3                                100        120
         200         3                               1000       87
         500         3                               1000       79
        1000                                800                   77
        1000        3                       800      700          77

        1000        45                      1000                    89


    */

}