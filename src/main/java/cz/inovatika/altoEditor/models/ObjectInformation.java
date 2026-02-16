package cz.inovatika.altoEditor.models;

import com.hp.hpl.jena.graph.query.SimpleQueryEngine;
import cz.inovatika.altoEditor.utils.Const;

public class ObjectInformation {

    private String pid;
    private String model;
    private String label;
    private String parentPath;
    private String parentLabel;

    public ObjectInformation() {
    }

    public ObjectInformation(String pid, String model, String label, String parentPid, String parentLabel) {
        this.pid = pid;
        this.model = transformModel(model);
        this.label = label;
        this.parentPath = fixParentPath(parentPid, pid);
        this.parentLabel = parentLabel;
    }

    private String transformModel(String model) {
        if (model == null || model.isEmpty()) {
            return null;
        }
        return Const.DIGITAL_OBJECT_MODEL_PAGE.equalsIgnoreCase(model) ? Const.DIGITAL_OBJECT_MODEL_PAGE : Const.DIGITAL_OBJECT_MODEL_OTHER;
    }

    private String fixParentPath(String parentPid, String pid) {
        if (parentPid == null) {
            return null;
        }
        parentPid = parentPid.replace(pid, "");
        if (parentPid == null || parentPid.isEmpty()) {
            return null;
        } else if (parentPid.endsWith("/")) {
            return parentPid.substring(0, parentPid.length() -1);
        } else {
            return parentPid;
        }
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
