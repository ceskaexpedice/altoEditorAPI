package cz.inovatika.altoEditor.response;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
public class AltoEditorResponse {

    private static final Logger LOG = Logger.getLogger(AltoEditorResponse.class.getName());

    public static final int STATUS_FAILURE = -1;
    public static final int STATUS_LOGIN_INCORRECT = -5;
    public static final int STATUS_LOGIN_REQUIRED = -7;
    public static final int STATUS_LOGIN_SUCCESS = -8;
    public static final int STATUS_MAX_LOGIN_ATTEMPTS_EXCEEDED = -6;
    public static final int STATUS_SERVER_TIMEOUT = -100;
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_TRANSPORT_ERROR = -90;
    public static final int STATUS_VALIDATION_ERROR = -4;

    @XmlElement(name = "data")
    private Object data;

    @XmlElement(name = "startRow")
    private Integer startRow;

    @XmlElement(name = "endRow")
    private Integer endRow;

    @XmlElement(name = "totalRow")
    private Integer totalRows;

    @XmlElement(name = "status")
    private int status;

    @XmlElement(name = "errors")
    private Object errors;

    public AltoEditorResponse() {}

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    public Integer getEndRow() {
        return endRow;
    }

    public void setEndRow(Integer endRow) {
        this.endRow = endRow;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getErrors() {
        return errors;
    }

    public void setErrors(Object errors) {
        this.errors = errors;
    }

    public AltoEditorResponse(Object singleItem) {
        this.status = STATUS_SUCCESS;
        if (singleItem instanceof List) {
            this.startRow = 0;
            List<Object> items = (List) singleItem;
            this.endRow = Math.max(0, items.size() -1);
            this.totalRows = items.size();
            this.data = items;
        } else {
            this.startRow = 0;
            if (singleItem == null) {
                this.endRow = 0;
                this.totalRows = 0;
                this.data = null;
            } else {
                this.endRow = 0;
                this.totalRows = 1;
                this.data = singleItem;
            }
        }
    }

    public AltoEditorResponse(int status, Integer startRow, Integer endRow, Integer totalRows, List<Object> data) {
        this.status = status;
        this.startRow = startRow;
        this.endRow = endRow;
        this.totalRows = totalRows;
        this.data = data;
    }

    public static AltoEditorResponse asError(Throwable ex) {
        return asError(ex.getMessage(), ex);
    }

    public static AltoEditorResponse asError(String message, Throwable ex) {
        AltoEditorResponse response = new AltoEditorResponse();
        response.setStatus(STATUS_FAILURE);
        if (message != null && message.contains("No configuration setting found for key")) {
            message = message.substring(message.indexOf("No configuration setting found for key"));
        }
        response.setErrors(Collections.singletonList(message));
        if (ex != null) {
            ex.printStackTrace();
        }
        return response;
    }
}
