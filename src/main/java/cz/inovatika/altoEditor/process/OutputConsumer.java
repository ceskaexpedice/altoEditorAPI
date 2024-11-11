package cz.inovatika.altoEditor.process;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads the external process output in separate thread.
 */
public class OutputConsumer extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(OutputConsumer.class.getName());
    private final InputStream input;
    private final StringBuilder output;
    private Throwable error;

    public OutputConsumer(InputStream input) {
        this.input = input;
        output = new StringBuilder();
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {
            for(String line; (line = reader.readLine()) != null;) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
        } catch (Throwable ex) {
            error = ex;
        } finally {
            LOGGER.debug("Close.");
            IOUtils.closeQuietly(reader);
        }
    }

    public String getOutput() {
        return output.toString();
    }

    public Throwable getError() {
        return error;
    }

}
