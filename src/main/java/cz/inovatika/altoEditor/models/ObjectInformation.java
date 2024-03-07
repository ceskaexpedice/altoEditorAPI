package cz.inovatika.altoEditor.models;

public class ObjectInformation {

    private String pid;
    private String label;
    private String parentPath;
    private String parentLabel;

    public ObjectInformation() {
    }

    public ObjectInformation(String pid, String label, String parentPid, String parentLabel) {
        this.pid = pid;
        this.label = label;
        this.parentPath = fixParentPath(parentPid, pid);
        this.parentLabel = parentLabel;
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
}
