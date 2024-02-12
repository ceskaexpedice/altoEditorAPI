package cz.inovatika.altoEditor.process;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * Runs an external process in own thread to handle possible process freeze.
 */
public class AsyncProcess extends Thread {

    private static final Logger LOG = Logger.getLogger(AsyncProcess.class.getName());
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
            LOG.fine("Done " + cmdLine);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, cmdLine.toString(), ex);
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
        Level level = isDone() ? Level.FINE : Level.WARNING;
        LOG.log(level, "Kill isDone: " + isDone() + ", " + cmdLine);
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
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

}
