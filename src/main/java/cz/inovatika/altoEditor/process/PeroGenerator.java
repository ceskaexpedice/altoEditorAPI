package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.utils.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeroGenerator extends ExternalProcess {

    private static final Logger LOGGER = Logger.getLogger(PeroGenerator.class.getName());

    private final File imageFile;
    private final File outputOcr;
    private final File outputAlto;

    public PeroGenerator(File imageFile, String ocrSuffix, String altoSuffix) {
        this.imageFile = imageFile;
        this.outputOcr = new File(imageFile.getAbsolutePath().substring(0, imageFile.getAbsolutePath().lastIndexOf(".")) + ocrSuffix);
        this.outputAlto = new File(imageFile.getAbsolutePath().substring(0, imageFile.getAbsolutePath().lastIndexOf(".")) + altoSuffix);
    }

//    public void run() {
//        if (!imageFile.exists()) {
//            throw new IllegalStateException(imageFile.getAbsolutePath() + " not exists!");
//        }
//        if (outputOcr.exists()) {
//            throw new IllegalStateException(outputOcr.getAbsolutePath() + " exists!");
//        }
//        if (outputAlto.exists()) {
//            throw new IllegalStateException(outputAlto.getAbsolutePath() + " exists!");
//        }
//
//        List<String> cmd = new ArrayList<>();
//        cmd.add(Config.getProcessorPeroExec());
//        cmd.add(Config.getProcessorPeroArg());
//        cmd.add("-i");
//        cmd.add(imageFile.getAbsolutePath());
//        cmd.add("-oO");
//        cmd.add(outputOcr.getAbsolutePath());
//        cmd.add("-oA");
//        cmd.add(outputAlto.getAbsolutePath());
//        cmd.add("-key");
//        cmd.add(Config.getProcessorPeroKey());
//
//        LOGGER.info(String.join(" ", cmd));
//
//        try {
//            Runtime r = Runtime.getRuntime();
//            String s;
//            Process p = r.exec(String.join(" ", cmd));
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//            StringWriter infoWriter = new StringWriter();
//            while ((s = in.readLine()) != null) {
//                System.out.println(s);
//                infoWriter.append(s).append("\n");
//            }
//            if (!infoWriter.toString().isEmpty()) {
//                LOGGER.info(infoWriter.toString());
//            }
//
//            StringWriter errorWriter = new StringWriter();
//            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            while ((s = error.readLine()) != null) {
//                System.out.println(s);
//                errorWriter.append(s).append("\n");
//            }
//            if (!errorWriter.toString().isEmpty()) {
//                LOGGER.severe(errorWriter.toString());
//            }
//        } catch (Exception ex) {
//            LOGGER.severe("Error during generating OCR and ALTO");
//        }
//    }


    @Override
    public void run() {
        if (!imageFile.exists()) {
            throw new IllegalStateException(imageFile.getAbsolutePath() + " not exists!");
        }
        if (outputOcr.exists()) {
            throw new IllegalStateException(outputOcr.getAbsolutePath() + " exists!");
        }
        if (outputAlto.exists()) {
            throw new IllegalStateException(outputAlto.getAbsolutePath() + " exists!");
        }
        super.run();
    }

    @Override
    protected List<String> buildCmdLine() {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(Config.getProcessorPeroExec());
        cmdLine.add(Config.getProcessorPeroArg());
        cmdLine.add("-i");
        cmdLine.add(imageFile.getAbsolutePath());
        cmdLine.add("-oO");
        cmdLine.add(outputOcr.getAbsolutePath());
        cmdLine.add("-oA");
        cmdLine.add(outputAlto.getAbsolutePath());
        cmdLine.add("-key");
        cmdLine.add(Config.getProcessorPeroKey());
        return cmdLine;
    }

    @Override
    public long getTimeout() {
        return Config.getProcessorPeroTimeout();
    }

    @Override
    public boolean isOk() {
        return outputOcr.exists() &&
                outputAlto.exists() && outputAlto.length() > 0;
    }
}
