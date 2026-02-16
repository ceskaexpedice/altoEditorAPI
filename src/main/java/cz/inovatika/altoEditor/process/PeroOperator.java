package cz.inovatika.altoEditor.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

        List<File> imageFiles = new ArrayList<>();

        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jpg")) {
                imageFiles.add(file);
            }
        }

        if (!imageFiles.isEmpty()) {
            try {
                generateAltoAndOcr(imageFiles, peroEngine, folder.getName());
            } catch (Exception e) {
                LOGGER.error("OCR generation failed for {}", folder.getAbsolutePath(), e);
            }
        } else {
            LOGGER.info("No JPG files found in {}", folder.getAbsolutePath());
        }
    }

    private void generateAltoAndOcr(List<File> imageFiles, Integer peroEngine, String folderName) throws IOException {

        if (imageFiles == null || imageFiles.isEmpty()) {
            LOGGER.warn("No images provided for OCR in {}", folderName);
            return;
        }

        PeroOcrProcessor peroOcrProcessor = new PeroOcrProcessor(peroEngine);
        try {
            boolean processed = peroOcrProcessor.generate(imageFiles, ".txt", ".xml");
            if (processed) {
                LOGGER.info("OCR GENERATED SUCCESSFULLY for {}.", folderName);
            } else {
                LOGGER.warn("OCR processing returned false for {}", folderName);
                throw new IOException("OCR generation failed for " + folderName);
            }
        } catch (JSONException ex) {
            LOGGER.error("Generating OCR for {} failed.", folderName, ex);
            throw new IOException("OCR generation failed for " + folderName, ex);
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
