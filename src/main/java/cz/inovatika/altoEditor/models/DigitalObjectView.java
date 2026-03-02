package cz.inovatika.altoEditor.models;

import java.sql.Timestamp;

public class DigitalObjectView {

    private Integer id = null;
    private String login = null;
    private String instance = null;
    private String pid = null;
    private String version = null;
    private Timestamp datum = null;
    private String state = null;
    private String label = null;
    private String parentPath = null;
    private String parentLabel = null;
    private Boolean lock = null;
    private String model = null;
    private Timestamp updateTime = null;

    public DigitalObjectView() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getDatum() {
        return datum;
    }

    public void setDatum(Timestamp datum) {
        this.datum = datum;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public void setParentLabel(String parentLabel) {
        this.parentLabel = parentLabel;
    }

    public Boolean getLock() {
        return lock;
    }

    public void setLock(Boolean lock) {
        this.lock = lock;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
