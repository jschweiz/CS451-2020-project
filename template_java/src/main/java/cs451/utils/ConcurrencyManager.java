package cs451.utils;

public class ConcurrencyManager {

    // number of thread working :) 
    public static final boolean REDUCE_THREADS = false;
    public static final int NUM_THREAD_PACKET_HANDLER = 2;


    // sets the time before resending a message if not ack
    public static final int SENDING_PERIOD_PACKET = 1000;
    public static final int SENDING_PERIOD_PING = 200;


    // recurrent function to execute
    public static final int CHECKING_PERIOD_PING = 15000; // check the lost processes
    public static final int CHECKING_PERIOD_MESSAGE_RECEIVED = 2000; // check toal number of messages received



    public static int findBestNumberOfReceiverThreads(int numProcesses) {
        // assume there are 15 threads available in general
        return (15 - numProcesses * 3) / numProcesses;
    }
}
