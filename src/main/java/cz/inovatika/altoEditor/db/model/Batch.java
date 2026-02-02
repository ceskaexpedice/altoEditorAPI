package cz.inovatika.altoEditor.db.model;

import java.sql.Timestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a batch entity with various attributes such as ID, state, priority, and timestamps for
 * creation and updates. This class provides getter and setter methods to retrieve and modify these
 * attributes.
 *
 * The Batch class is designed to handle information related to a batch, including metadata like
 * type, instance, and logging details. These attributes can be utilized to manage and track the
 * state and progress of a batch within a system.
 */
public class Batch {

    protected static final Logger LOGGER = LogManager.getLogger(Batch.class.getName());

    public Integer id = null;
    public String pid = null;
    public Timestamp createDate = null;
    public Timestamp updateDate = null;
    public String state = null;
    public String substate = null;
    public String priority = null;
    public String type = null;
    public String instance = null;
    public Integer objectId = null;
    public Integer estimateItemNumber = null;
    public String log = null;


    public Integer getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
    }

    public String getState() {
        return state;
    }

    public String getSubstate() {
        return substate;
    }

    public String getType() {
        return type;
    }

    public String getInstance() {
        return instance;
    }

    public Integer getEstimateItemNumber() {
        return estimateItemNumber;
    }

    public String getLog() {
        return log;
    }

    public String getPriority() {
        return priority;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSubstate(String substate) {
        this.substate = substate;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }

    public void setEstimateItemNumber(Integer estimateItemNumber) {
        this.estimateItemNumber = estimateItemNumber;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
