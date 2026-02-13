package cz.inovatika.altoEditor.process;

import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;

/**
 * The {@code PeroOperator} class is responsible for generating OCR results and ALTO files
 * from image files located in a specified folder. It utilizes a designated OCR engine
 * to process images and produces both textual and XML-based outputs.
 */
public class PeroOperator {

    private static final Logger LOGGER = LogManager.getLogger(PeroOperator.class.getName());

    public PeroOperator() {
    }

    public Result generate(File folder, Integer peroEngine) {
        Result result = new Result();
        try {
            if (folder != null && folder.exists()) {
                generateAlto(folder, peroEngine);
            } else {
                throw new IllegalStateException("Folder " + (folder == null ? null : folder.getAbsolutePath()) + " does not exists");
            }
        } catch (Exception ex) {
            result.setException(ex);
        }
        return result;
    }

    protected void generateAlto(File folder, Integer peroEngine) throws IOException {
        if (folder == null || !folder.exists() || !folder.canRead() || !folder.canWrite()) {
            throw new IOException("It is not possible to access " + (folder == null ? null : folder.getAbsolutePath()));
        }

        File[] files = folder.listFiles();
        if (files == null) {
            throw new IOException("Unable to list files in " + folder.getAbsolutePath());
        }

        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jpg")) {
                try {
                    generateAltoAndOcr(file, peroEngine);
                } catch (Exception e) {
                    LOGGER.error("OCR generation failed for {}", file.getAbsolutePath(), e);
                }
            }
        }
    }

    private void generateAltoAndOcr(File imageFile, Integer peroEngine) throws IOException {

        PeroOcrProcessor peroOcrProcessor = new PeroOcrProcessor(peroEngine);
        try {
            boolean processed = peroOcrProcessor.generate(imageFile, ".txt", ".xml");
            if (processed) {
                LOGGER.info("OCR GENERATED SUCCESSFULLY for {}.", imageFile.getAbsolutePath());
            } else {
                LOGGER.warn("OCR processing returned false for {}", imageFile.getAbsolutePath());
            }
        } catch (JSONException ex) {
            LOGGER.error("Generating OCR for {} failed.", imageFile.getName(), ex);
            throw new IOException("OCR generation failed for " + imageFile.getName(), ex);
        }
    }

    public static class Result {
        private File file;
        private Exception ex;
        private String message;

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public Exception getException() {
            return ex;
        }

        public void setException(Exception ex) {
            this.ex = ex;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
