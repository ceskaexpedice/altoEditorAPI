package cz.inovatika.altoEditor.models;

import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.db.model.User;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import java.sql.Timestamp;

public class DigitalObjectView {

    private Integer id = null;
    private String userLogin = null;
    private String instance = null;
    private String pid = null;
    private String version = null;
    private Timestamp datum = null;
    private String state = null;
    private String label = null;
    private String parentPath = null;
    private String parentLabel = null;
    private Boolean lock = null;

    public DigitalObjectView() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
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
}
