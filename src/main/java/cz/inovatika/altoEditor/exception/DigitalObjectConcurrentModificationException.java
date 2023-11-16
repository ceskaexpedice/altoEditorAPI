package cz.inovatika.altoEditor.exception;

public class DigitalObjectConcurrentModificationException extends AltoEditorException {

    private static final long serialVersionUID = 1L;

    public DigitalObjectConcurrentModificationException(String pid, String message) {
        super(pid, message);
    }

    public DigitalObjectConcurrentModificationException(String pid, Integer batchId, String message, Throwable cause) {
        super(pid, batchId, null, message, cause);
    }
}
