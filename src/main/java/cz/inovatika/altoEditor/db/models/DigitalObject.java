package cz.inovatika.altoEditor.db.models;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigitalObject {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DigitalObject.class.getName());

    private Integer id = null;
    private Integer rUserId = null;
    private String instance = null;
    private String pid = null;
    private String label = null;
    private String parentPath = null;
    private String parentLabel = null;
    private String version = null;
    private Timestamp datum = null;
    private String state = null;
    private Boolean lock = null;

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

    public DigitalObject(ResultSet rs) {
        try {
            if (rs != null) {
                final ResultSetMetaData metaData = rs.getMetaData();
                HashMap resultFields = new HashMap();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    resultFields.put(metaData.getColumnName(i).toUpperCase(), metaData.getColumnType(i));
                }
                Field[] declaredFields = DigitalObject.class.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    String f = declaredField.getName().toUpperCase();
                    if (resultFields.containsKey(f)) {
                        if (resultFields.get(f).equals(Types.INTEGER)) {
                            declaredField.set(this, rs.getInt(f));
                        } else if (resultFields.get(f).equals(Types.TIMESTAMP)) {
                            declaredField.set(this, rs.getTimestamp(f));
                        } else {
                            declaredField.set(this, rs.getString(f));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Chyba nacteni digitalniho objektu z DB {}", id);
            ex.printStackTrace();
        }
    }
}
