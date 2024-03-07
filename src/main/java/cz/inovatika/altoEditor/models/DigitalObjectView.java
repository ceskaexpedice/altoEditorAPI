package cz.inovatika.altoEditor.models;

import cz.inovatika.altoEditor.db.models.DigitalObject;
import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import java.sql.Timestamp;

public class DigitalObjectView {

    public Integer id = null;
    public String userLogin = null;
    public String instance = null;
    public String pid = null;
    public String versionXml = null;
    public Timestamp datum = null;
    public String state = null;
    private String label = null;
    private String parentPath = null;
    private String parentLabel = null;


    public DigitalObjectView(DigitalObject digitalObject, User user) {
        this.id = digitalObject.getId();
        this.instance = digitalObject.getInstance();
        this.pid = digitalObject.getPid();
        this.versionXml = AltoDatastreamEditor.ALTO_ID + "." + digitalObject.getVersion();
        this.datum = digitalObject.getDatum();
        this.state = digitalObject.getState();
        this.label = digitalObject.getLabel();
        this.parentPath = digitalObject.getParentPath();
        this.parentLabel = digitalObject.getParentLabel();

        this.userLogin = user.getLogin();
    }

    public Integer getId() {
        return id;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getInstance() {
        return instance;
    }

    public String getPid() {
        return pid;
    }

    public String getVersionXml() {
        return versionXml;
    }

//    public String getVersionId() {
//        return versionId;
//    }

    public Timestamp getDatum() {
        return datum;
    }

    public String getState() {
        return state;
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
}
