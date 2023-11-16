package cz.inovatika.altoEditor.models;

import cz.inovatika.altoEditor.utils.Config;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "info")
public class ApplicationVersion {

    private String application;
    private String version;

    public ApplicationVersion() {
        this.application = "AltoEditor";
        this.version = Config.getVersion();
    }

    public String getVersion() {
        return version;
    }

    public String getApplication() {
        return application;
    }
}
