package cz.inovatika.altoEditor.db.models;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Version {

    protected static final Logger LOGGER = LogManager.getLogger(Version.class.getName());

    private Integer id = null;
    private Timestamp datum = null;
    private Integer version = null;

    public Version(ResultSet rs) {
        try {
            if (rs != null) {
                final ResultSetMetaData metaData = rs.getMetaData();
                HashMap resultFields = new HashMap();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    resultFields.put(metaData.getColumnName(i).toUpperCase(), metaData.getColumnType(i));
                }
                Field[] declaredFields = Version.class.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    String f = declaredField.getName().toUpperCase();
                    if (resultFields.containsKey(f)) {
                        if (resultFields.get(f).equals(Types.INTEGER)) {
                            declaredField.set(this, rs.getInt(f));
                        } else if (resultFields.get(f).equals(Types.BIGINT)) {
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
            LOGGER.error("Chyba nacteni verze z DB {}", id);
            ex.printStackTrace();
        }
    }
}
