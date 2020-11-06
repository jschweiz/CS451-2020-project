package cs451;

import java.io.IOException;
import java.net.SocketException;

public class Main {

    public static Coordinator coord = null;

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Process.writeInFile();
                    // Main.coord.finishedBroadcasting();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws InterruptedException, SocketException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
        System.out.println("List of hosts is:");
        for (Host host: parser.hosts()) {
            if (parser.myId() == host.getId()) {
                parser.populateOwnProcess(host);
            }
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }


        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());
        coord = coordinator;

        System.out.println("Waiting for all processes for finish initialization");
            coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        Process process = new Process(parser);
        process.broadcastMessages();

        System.out.println("Signaling end of broadcasting messages");
            coordinator.finishedBroadcasting();

    }

}
