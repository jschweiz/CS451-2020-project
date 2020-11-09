package cs451.unused;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class LogManager extends Formatter {

    @Override
    public String format(final LogRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("looooooooooo").append(System.getProperty("line.separator"));
        return sb.toString();
    }

    public static Logger getLogger(Class c) {
        
        Logger LOGGER = Logger.getLogger(c.getName());

        Formatter formatter = new LogManager();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        LOGGER.addHandler(consoleHandler);

        return LOGGER;
    }
}
