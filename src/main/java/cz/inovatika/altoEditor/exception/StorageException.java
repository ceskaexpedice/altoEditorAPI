package cz.inovatika.altoEditor.exception;

public class StorageException extends AltoEditorException {

    public StorageException(String pid) {
        this(pid, (Throwable) null);
    }

    public StorageException(String pid, String message) {
        this(pid, message, null);
    }

    public StorageException(String pid, Throwable cause) {
        this(pid, cause == null ? null : cause.getMessage(), cause);
    }

    public StorageException(String pid, String message, Throwable cause) {
        this(pid, null, null, message, cause);
    }

    public StorageException(String pid, Integer batchId, String dsId, String message, Throwable cause) {
        super(pid, batchId, dsId, message, cause);
    }
}
