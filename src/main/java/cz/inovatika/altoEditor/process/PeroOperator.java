package cz.inovatika.altoEditor.process;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class PeroOperator {

    private static final Logger LOGGER = Logger.getLogger(PeroOperator.class.getName());

    public PeroOperator() {
    }

    public Result generate(File folder) {
        Result result = new Result();
        try {
            if (folder != null && folder.exists()) {
                generateAlto(folder);
            } else {
                throw new IllegalStateException("Folder " + folder.getAbsolutePath() + " does not exists");
            }
        } catch (Exception ex) {
            result.setException(ex);
        } finally {
            return result;
        }
    }

    protected void generateAlto(File folder) throws IOException {
        if (folder == null || !folder.exists() || !folder.canRead() || !folder.canWrite()) {
            throw new IOException("It is not possible to access " + (folder == null ? null : folder.getAbsolutePath()));
        }
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith("jpg")) {
                File imgFile = file;
                generateAltoAndOcr(imgFile);
            }
        }
    }

    private void generateAltoAndOcr(File imageFile) throws IOException {

        PeroGenerator process = new PeroGenerator(imageFile, ".txt", ".xml");

        if (process != null) {
            process.run();

            if (!process.isOk()) {
                throw new IOException("Generating OCR for " + imageFile.getName() + " failed.");
            }
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
