package cz.inovatika.altoEditor.db.model;

import java.sql.Timestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a version entity with attributes such as ID, version number, and timestamp.
 *
 * This class provides getter and setter methods to retrieve and modify these attributes.
 * It is designed to handle information related to a specific version instance, including
 * its identifier, version number, and the date and time associated with this version.
 */
public class Version {

    protected static final Logger LOGGER = LogManager.getLogger(Version.class.getName());

    private Integer id = null;
    private Timestamp datum = null;
    private Integer version = null;

    public Version() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getDatum() {
        return datum;
    }

    public void setDatum(Timestamp datum) {
        this.datum = datum;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
