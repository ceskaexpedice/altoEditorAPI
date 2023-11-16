package cz.inovatika.altoEditor.db;

import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;

public class DigitalObject {

    public Integer id = null;
    public Integer rUserId = null;
    public String instance = null;
    public String pid = null;
    public String version = null;
    public Timestamp datum = null;
    public String state = null;

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
            Dao.LOG.error("Chyba nacteni digitalniho objektu z DB {}", id);
            ex.printStackTrace();
        }
    }

    public static DigitalObject getObjectWithMaxVersion(List<DigitalObject> digitalObjects) {
        DigitalObject objectWithMaxVersion = null;
        for (DigitalObject digitalObject : digitalObjects) {
            if (objectWithMaxVersion == null) {
                objectWithMaxVersion = digitalObject;
            } else {
                Integer maxVersionId = AltoDatastreamEditor.getVersionId(objectWithMaxVersion.getVersion());
                Integer currentVersionId = AltoDatastreamEditor.getVersionId(objectWithMaxVersion.getVersion());
                if (currentVersionId > maxVersionId) {
                    objectWithMaxVersion = digitalObject;
                }
            }
        }
        return objectWithMaxVersion;
    }
}
