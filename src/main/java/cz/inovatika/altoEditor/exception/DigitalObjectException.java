package cz.inovatika.altoEditor.exception;

public class DigitalObjectException extends AltoEditorException {

    private static final long serialVersionUID = 1L;

    public DigitalObjectException(String pid, String message) {
        super(pid, message);
    }

    public DigitalObjectException(String pid, Throwable cause) {
        super(pid, cause == null ? null : cause.getMessage(), cause);
    }

    public DigitalObjectException(String pid, Integer batchId, String message, Throwable cause) {
        super(pid, batchId, null, message, cause);
    }

    public DigitalObjectException(String pid, String message, Throwable cause) {
        super(pid, message, cause);
    }
}
