
package cz.inovatika.altoEditor.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * The helper to run external processes.
 */
public class ExternalProcess implements Runnable {

    private static final Logger LOG = Logger.getLogger(ExternalProcess.class.getName());
    public static final long DEFAULT_TIMEOUT = 2 * 60 * 1000;
    public static final int DEFAULT_RETRY_ATTEMPTS = 0;

    private AsyncProcess asyncProcess;

    protected ExternalProcess() {
    }

    @Override
    public void run() {
        List<String> cmdLine = buildCmdLine();
        try {
            int numberOfAttemps = 1;
            for (int i = 0; i < numberOfAttemps; i++) {
                runCmdLine(cmdLine);
                if (isOk()) {
                    return ;
                }
                LOG.log(Level.WARNING, "{0}. failure, \n{1}, \nCmd: {2}",
                        new Object[]{i + 1, getFullOutput(), cmdLine});
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected List<String> buildCmdLine() {
        List<String> cmdLine = new ArrayList<>();
        return cmdLine;
    }

    private int runCmdLine(List<String> cmdLine) throws IOException, InterruptedException {
        StringBuilder debug = new StringBuilder();
        for (String arg : cmdLine) {
            debug.append(arg).append(" ");
        }
        LOG.fine("run: " + debug);
        asyncProcess = new AsyncProcess(cmdLine);
        asyncProcess.start();
        long timeout = getTimeout();
        asyncProcess.join(timeout);
        asyncProcess.kill();
        LOG.fine(getFullOutput());
        return asyncProcess.getExitCode();
    }

    public String getOut() {
        return asyncProcess == null ? null: asyncProcess.getOut();
    }

    public String getErr() {
        return null;
    }

    public int getExitCode() {
        return asyncProcess == null ? -1: asyncProcess.getExitCode();
    }

    public boolean isOk() {
        return getExitCode() == 0;
    }

    public String getFullOutput() {
        return String.format("exit: %s,\nout: %s", getExitCode(), getOut());
    }

    public long getTimeout() {
        return DEFAULT_TIMEOUT;
    }

}
