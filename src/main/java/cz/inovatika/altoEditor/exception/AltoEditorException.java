package cz.inovatika.altoEditor.exception;

public class AltoEditorException extends Exception{

    private static final long serialVersionUID = 1L;

    private final String pid;
    private final Integer batchId;
    private String message;
    private final String dsId;

    public AltoEditorException(String pid) {
        this(pid, (Throwable) null);
    }

    public AltoEditorException(String pid, String message) {
        this(pid, message, null);
    }

    public AltoEditorException(String pid, Throwable cause) {
        this(pid, cause == null ? null : cause.getMessage(), cause);
    }

    public AltoEditorException(String pid, String message, Throwable cause) {
        this(pid, null, null, message, cause);
    }

    public AltoEditorException(String pid, Integer batchId, String dsId, String message, Throwable cause) {
        super(buildMsg(pid, batchId, dsId, message), cause);
        this.message = message;
        this.pid = pid;
        this.batchId = batchId;
        this.dsId = dsId;
    }

    public String getPid() {
        return pid;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public String getDsId() {
        return dsId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMyMessage() {
        return message;
    }

    private static String buildMsg(String pid, Integer batchId, String dsId, String message) {
        StringBuilder sb = new StringBuilder("PID: ").append(pid);
        if (batchId != null) {
            sb.append(", batchId: ").append(batchId);
        }
        if (dsId != null) {
            sb.append(", dsId: ").append(dsId);
        }
        if (message != null) {
            sb.append(", ").append(message);
        }
        return sb.toString();
    }
}
