package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.filter.DigitalObjectFilter;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Concrete implementation of the {@link DigitalObjectDao} interface for managing {@link DigitalObject}
 * entities using the Empire database framework. This class provides methods to create,
 * retrieve, update, and delete batch records while handling database interactions.
 * <p>
 * This DAO operates over the {@link AltoEditorDatabase.DigitalObjectTable} and uses the Empire
 * database utilities to execute database commands and manage transactions.
 */
public class EmpireDigitalObjectDao extends EmpireDao implements DigitalObjectDao {

    protected static final Logger LOGGER = LogManager.getLogger(EmpireDigitalObjectDao.class.getName());
    private final AltoEditorDatabase.DigitalObjectTable table;

    public EmpireDigitalObjectDao(AltoEditorDatabase db) {
        super(db);
        table = db.tableDigitalObject;
    }

    @Override
    public DigitalObject createDigitalObject() {
        return new DigitalObject();
    }

    @Override
    public void update(DigitalObject digitalObject) throws ConcurrentModificationException {
        DBRecord record = new DBRecord();
        Connection connection = getConnection();

        if (digitalObject.getId() == null) {
            record.create(table);
        } else {
            record.read(table, digitalObject.getId(), connection);
        }
        digitalObject.setDatum(new Timestamp(System.currentTimeMillis()));
        record = setBeanProperties(digitalObject, record);

        try {
            record.update(connection);
        } catch (RecordUpdateInvalidException e) {
            throw new ConcurrentModificationException(e);
        }
        getBeanProperties(record, digitalObject);
    }

    @Override
    public Optional<DigitalObject> findById(Integer digitalObjectId) {
        DBRecord record = new DBRecord();
        try {
            record.read(table, digitalObjectId, getConnection());
            return getBeanProperties(record, null);
        } catch (RecordNotFoundException ex) {
            return Optional.empty();
        } finally {
            record.close();
        }
    }

    @Override
    public List<DigitalObjectView> findByFilter(DigitalObjectFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        AltoEditorDatabase.UserTable tableUser = database.tableUser;

        cmd.select(table.id, table.rUserId, table.instance, table.pid, table.label, table.parentPath, table.parentLabel, table.version, table.datum, table.state, table.lock, table.model, table.updateTime);
        cmd.select(tableUser.login);
        cmd.join(table.rUserId, tableUser.id);

        if (filter.getId() != null && !filter.getId().isEmpty()) {
            cmd.where(table.id.in(filter.getId()));
        }
        if (filter.getrUserId() != null) {
            cmd.where(table.rUserId.is(filter.getrUserId()));
        }
        if (filter.getInstance() != null) {
            cmd.where(table.instance.is(filter.getInstance()));
        }
        if (filter.getPid() != null && !filter.getPid().isEmpty()) {
            cmd.where(table.pid.in(filter.getPid()));
        }
        if (filter.getLabel() != null) {
            cmd.where(table.label.is(filter.getLabel()));
        }
        if (filter.getParentPath() != null) {
            cmd.where(table.parentPath.is(filter.getParentPath()));
        }
        if (filter.getParentLabel() != null) {
            cmd.where(table.parentLabel.is(filter.getParentLabel()));
        }
        if (filter.getVersion() != null) {
            cmd.where(table.version.is(filter.getVersion()));
        }
        if (filter.getDatumFrom() != null) {
            cmd.where(table.datum.isMoreOrEqual(filter.getDatumFrom()));
        }
        if (filter.getDatumTo() != null) {
            cmd.where(table.datum.isLessOrEqual(filter.getDatumTo()));
        }
        if (filter.getDatum() != null) {
            cmd.where(table.datum.is(filter.getDatum()));
        }
        if (filter.getState() != null) {
            cmd.where(table.state.is(filter.getState()));
        }
        if (filter.getLock() != null) {
            cmd.where(table.lock.is(filter.getLock()));
        }
        if (filter.getLogin() != null) {
            cmd.where(tableUser.login.is(filter.getLogin()));
        }
        if (filter.getModel() != null) {
            cmd.where(table.model.is(filter.getModel()));
        }

        EmpireUtils.addOrderBy(cmd, filter.getOrderBy(), filter.getOrderSort(), table.id, true);
        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());
            if (!reader.skipRows(filter.getOffset())) {
                return Collections.emptyList();
            }
            ArrayList<DigitalObjectView> digitalObjectes = new ArrayList<DigitalObjectView>(filter.getLimit());
            for (Iterator<DBRecordData> it = reader.iterator(filter.getLimit()); it.hasNext(); ) {
                DBRecordData rec = it.next();
                DigitalObjectView digitalObject = new DigitalObjectView();
                rec.setBeanProperties(digitalObject);

                digitalObjectes.add(digitalObject);
            }
            return digitalObjectes;
        } finally {
            reader.close();
        }
    }

    @Override
    public int countByFilter(DigitalObjectFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        AltoEditorDatabase.UserTable tableUser = database.tableUser;

        cmd.select(table.count());
        cmd.join(table.rUserId, tableUser.id);

        if (filter.getId() != null && !filter.getId().isEmpty()) {
            cmd.where(table.id.in(filter.getId()));
        }
        if (filter.getrUserId() != null) {
            cmd.where(table.rUserId.is(filter.getrUserId()));
        }
        if (filter.getInstance() != null) {
            cmd.where(table.instance.is(filter.getInstance()));
        }
        if (filter.getPid() != null && !filter.getPid().isEmpty()) {
            cmd.where(table.pid.in(filter.getPid()));
        }
        if (filter.getLabel() != null) {
            cmd.where(table.label.is(filter.getLabel()));
        }
        if (filter.getParentPath() != null) {
            cmd.where(table.parentPath.is(filter.getParentPath()));
        }
        if (filter.getParentLabel() != null) {
            cmd.where(table.parentLabel.is(filter.getParentLabel()));
        }
        if (filter.getVersion() != null) {
            cmd.where(table.version.is(filter.getVersion()));
        }
        if (filter.getDatumFrom() != null) {
            cmd.where(table.datum.isMoreOrEqual(filter.getDatumFrom()));
        }
        if (filter.getDatumTo() != null) {
            cmd.where(table.datum.isLessOrEqual(filter.getDatumTo()));
        }
        if (filter.getDatum() != null) {
            cmd.where(table.datum.is(filter.getDatum()));
        }
        if (filter.getState() != null) {
            cmd.where(table.state.is(filter.getState()));
        }
        if (filter.getLock() != null) {
            cmd.where(table.lock.is(filter.getLock()));
        }
        if (filter.getLogin() != null) {
            cmd.where(tableUser.login.is(filter.getLogin()));
        }
        if (filter.getModel() != null) {
            cmd.where(table.model.is(filter.getModel()));
        }

        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());

            if (reader.moveNext()) {
                return reader.getInt(0);
            }
            return 0;
        } finally {
            reader.close();
        }
    }

    @Override
    public void deleteById(Integer digitalObjectId) {
        DBCommand cmd = database.createCommand();
        cmd.where(table.id.is(digitalObjectId));
        database.executeDelete(table, cmd, getConnection());
    }

    private DBRecord setBeanProperties(DigitalObject digitalObject, DBRecord record) {
        if (digitalObject != null) {
            record.setValue(table.pid, digitalObject.getPid());
            record.setValue(table.rUserId, digitalObject.getrUserId());
            record.setValue(table.instance, digitalObject.getInstance());
            record.setValue(table.label, digitalObject.getLabel());
            record.setValue(table.parentPath, digitalObject.getParentPath());
            record.setValue(table.parentLabel, digitalObject.getParentLabel());
            record.setValue(table.version, digitalObject.getVersion());
            record.setValue(table.datum, digitalObject.getDatum());
            record.setValue(table.updateTime, digitalObject.getUpdateTime());
            record.setValue(table.state, digitalObject.getState());
            record.setValue(table.lock, digitalObject.getLock());
            record.setValue(table.model, digitalObject.getModel());
            return record;
        } else {
            return record;
        }

    }

    private Optional<DigitalObject> getBeanProperties(DBRecordData record, DigitalObject existingDigitalObject) {
        DigitalObject targetDigitalObject = Optional.ofNullable(existingDigitalObject).orElseGet(DigitalObject::new);
        record.setBeanProperties(targetDigitalObject);
        return Optional.of(targetDigitalObject);
    }
}
