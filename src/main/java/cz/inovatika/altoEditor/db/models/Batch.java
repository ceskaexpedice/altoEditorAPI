package cz.inovatika.altoEditor.db.models;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Batch {

    protected static final Logger LOGGER = LogManager.getLogger(Batch.class.getName());

    public Integer id = null;
    public String pid = null;
    public Timestamp createDate = null;
    public Timestamp updateDate = null;
    public String state = null;
    public String substate = null;
    public String priority = null;
    public String type = null;
    public String instance = null;
    public Integer objectId = null;
    public Integer estimateItemNumber = null;
    public String log = null;


    public Integer getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
    }

    public String getState() {
        return state;
    }

    public String getSubstate() {
        return substate;
    }

    public String getType() {
        return type;
    }

    public String getInstance() {
        return instance;
    }

    public Integer getEstimateItemNumber() {
        return estimateItemNumber;
    }

    public String getLog() {
        return log;
    }

    public String getPriority() {
        return priority;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public Batch(ResultSet rs) {
        try {
            if (rs != null) {
                final ResultSetMetaData metaData = rs.getMetaData();
                HashMap resultFields = new HashMap();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    resultFields.put(metaData.getColumnName(i).toUpperCase(), metaData.getColumnType(i));
                }
                Field[] declaredFields = Batch.class.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    String f = declaredField.getName().toUpperCase();
                    if (resultFields.containsKey(f)) {
                        if (resultFields.get(f).equals(Types.INTEGER)) {
                            declaredField.set(this, rs.getInt(f));
                        } else if (resultFields.get(f).equals(Types.TIMESTAMP)) {
                            declaredField.set(this, rs.getTimestamp(f));
                        } else if (resultFields.get(f).equals(Types.VARCHAR)) {
                            declaredField.set(this, rs.getString(f));
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
