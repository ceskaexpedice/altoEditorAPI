package cz.inovatika.altoEditor.models;

import cz.inovatika.altoEditor.db.DigitalObject;
import cz.inovatika.altoEditor.db.User;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import java.sql.Timestamp;

public class DigitalObjectView {

    public Integer id = null;
    public String userLogin = null;
    public String instance = null;
    public String pid = null;
    public String versionXml = null;
//    public String versionId = null;
    public Timestamp datum = null;
    public String state = null;


    public DigitalObjectView(DigitalObject digitalObject, User user) {
        this.id = digitalObject.getId();
        this.instance = digitalObject.getInstance();
        this.pid = digitalObject.getPid();
        this.versionXml = AltoDatastreamEditor.ALTO_ID + "." + digitalObject.getVersion();
//        this.versionId = digitalObject.getVersion();
        this.datum = digitalObject.getDatum();
        this.state = digitalObject.getState();

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
}
