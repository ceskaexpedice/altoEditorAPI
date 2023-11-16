package cz.inovatika.altoEditor.exception;

public class DigitalObjectExistException extends AltoEditorException {

    private static final long serialVersionUID = 1L;

    public DigitalObjectExistException(String pid, Integer batchId, String message, Throwable cause) {
        super(pid, batchId, null, message, cause);
    }
}
