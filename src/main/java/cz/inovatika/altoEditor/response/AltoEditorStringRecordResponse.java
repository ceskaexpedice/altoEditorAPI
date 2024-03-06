package cz.inovatika.altoEditor.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static cz.inovatika.altoEditor.response.AltoEditorResponse.STATUS_FAILURE;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.STATUS_SUCCESS;

@XmlRootElement(name = "record")
@XmlAccessorType(XmlAccessType.FIELD)
public class AltoEditorStringRecordResponse {

    @XmlElement(name = "pid")
    private String pid;

    @XmlElement(name = "instanceId")
    private String instanceId;

    @XmlElement(name = "timestamp")
    private long timestamp;

    @XmlElement(name = "version")
    private String version;

    @XmlElement(name = "content")
    private String content;

    @XmlElement(name = "data")
    private Object data;

    @XmlElement(name = "status")
    private int status;

    public AltoEditorStringRecordResponse() {
        this.status = STATUS_SUCCESS;
    }

    public AltoEditorStringRecordResponse(String content, long timestamp, String pid) {
        this.status = STATUS_SUCCESS;
        this.content = content;
        this.timestamp = timestamp;
        this.pid = pid;
    }

    public AltoEditorStringRecordResponse(Throwable t) {
        this.status = STATUS_FAILURE;
        this.data = t;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
