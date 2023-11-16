package cz.inovatika.altoEditor.exception;

public class DigitalObjectNotFoundException extends AltoEditorException {

    private static final long serialVersionUID = 1L;

    public DigitalObjectNotFoundException(String pid, String message) {
        super(pid, message);
    }

    public DigitalObjectNotFoundException(String pid, Throwable cause) {
        super(pid, cause == null ? null : cause.getMessage(), cause);
    }

    public DigitalObjectNotFoundException(String pid, Integer batchId, String message, Throwable cause) {
        super(pid, batchId, null, message, cause);
    }
}
