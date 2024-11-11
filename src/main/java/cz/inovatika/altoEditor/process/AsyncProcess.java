package cz.inovatika.altoEditor.process;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs an external process in own thread to handle possible process freeze.
 */
public class AsyncProcess extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(AsyncProcess.class.getName());
    private final List<String> cmdLine;
    private AtomicReference<Process> refProcess = new AtomicReference<Process>();
    private AtomicBoolean done = new AtomicBoolean();
    private int exitCode;
    private OutputConsumer outputConsumer;

    public AsyncProcess(List<String> cmdLine) {
        this.cmdLine = cmdLine;
    }

    @Override
    public void run() {
        done.set(false);
        outputConsumer = null;
        exitCode = -1;
        ProcessBuilder pb = new ProcessBuilder(cmdLine);
        // for now redirect outputs into a single stream to eliminate
        // the need to run multiple threads to read each output
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            refProcess.set(process);
            outputConsumer = new OutputConsumer(process.getInputStream());
            outputConsumer.start();
            exitCode = process.waitFor();
            LOGGER.debug("Done " + cmdLine);
        } catch (Exception ex) {
            LOGGER.error(cmdLine.toString(), ex);
        } finally {
            done.set(true);
        }
    }

    public boolean isDone() {
        return done.get();
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOut() {
        return outputConsumer != null ? outputConsumer.getOutput() : "";
    }

    public void kill() {
        Level level = isDone() ? Level.DEBUG : Level.WARN;
        LOGGER.log(level, "Kill isDone: " + isDone() + ", " + cmdLine);
        Process process = refProcess.getAndSet(null);
        if (process != null) {
            process.destroy();
            IOUtils.closeQuietly(process.getInputStream());
            IOUtils.closeQuietly(process.getErrorStream());
            IOUtils.closeQuietly(process.getOutputStream());
            done.set(true);
            try {
                outputConsumer.join();
            } catch (InterruptedException ex) {
                LOGGER.error(ex);
            }
        }
    }
}
