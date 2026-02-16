package cz.inovatika.altoEditor.db.model;

import java.sql.Timestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a digital object with various metadata attributes such as ID, user ID,
 * instance identifier, persistent ID, label, parent details, version information,
 * timestamp, state, and lock status.
 *
 * This class provides methods to access and modify these attributes, allowing it
 * to act as a fundamental data structure for handling and storing information about
 * digital objects within a system.
 */
public class DigitalObject {

    protected static final Logger LOGGER = LogManager.getLogger(DigitalObject.class.getName());

    private Integer id = null;
    private Integer rUserId = null;
    private String instance = null;
    private String pid = null;
    private String label = null;
    private String parentPath = null;
    private String parentLabel = null;
    private String version = null;
    private Timestamp datum = null;
    private Timestamp updateTime = null;
    private String state = null;
    private Boolean lock = null;
    private String model;

    public Integer getId() {
        return id;
    }

    public Integer getrUserId() {
        return rUserId;
    }

    public String getPid() {
        return pid;
    }

    public String getVersion() {
        return version;
    }

    public Timestamp getDatum() {
        return datum;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public String getState() {
        return state;
    }

    public String getInstance() {
        return instance;
    }

    public String getLabel() {
        return label;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public Boolean getLock() {
        return lock == null ? false : lock;
    }

    public String getModel() {
        return model;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setrUserId(Integer rUserId) {
        this.rUserId = rUserId;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public void setParentLabel(String parentLabel) {
        this.parentLabel = parentLabel;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDatum(Timestamp datum) {
        this.datum = datum;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setLock(Boolean lock) {
        this.lock = lock;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
