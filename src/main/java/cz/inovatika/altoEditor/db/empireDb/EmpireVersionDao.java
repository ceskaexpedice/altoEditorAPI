package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.VersionDao;
import cz.inovatika.altoEditor.db.filter.VersionFilter;
import cz.inovatika.altoEditor.db.model.Version;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the {@link VersionDao} interface for managing {@link Version} entities
 * within the Empire database framework. This class provides mechanisms for creating,
 * retrieving, updating, filtering, and deleting version records.
 */
public class EmpireVersionDao extends EmpireDao implements VersionDao {

    protected static final Logger LOGGER = LogManager.getLogger(EmpireVersionDao.class.getName());
    private final AltoEditorDatabase.VersionTable table;

    public EmpireVersionDao(AltoEditorDatabase db) {
        super(db);
        table = db.tableVersion;
    }

    @Override
    public Version createVersion() {
        return new Version();
    }

    @Override
    public void update(Version version) throws ConcurrentModificationException {
        DBRecord record = new DBRecord();
        Connection connection = getConnection();

        if (version.getId() == null) {
            record.create(table);
        } else {
            record.read(table, version.getId(), connection);
        }
        record.setValue(table.version, version.getVersion());

        for (DBColumn col : table.getColumns()) {
            Object value = record.getValue(col);
            LOGGER.info("{} = {}", col.getName(), value);
        }

        try {
            record.update(connection);
        } catch (RecordUpdateInvalidException e) {
            throw new ConcurrentModificationException(e);
        }
        getBeanProperties(record, version);
    }

    @Override
    public Optional<Version> findById(Integer versionId) {
        DBRecord record = new DBRecord();
        try {
            record.read(table, versionId, getConnection());
            return getBeanProperties(record, null);
        } catch (RecordNotFoundException ex) {
            return Optional.empty();
        } finally {
            record.close();
        }
    }

    @Override
    public List<Version> findByFilter(VersionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.id, table.datum, table.version);
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
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
        if (filter.getVersion() != null) {
            cmd.where(table.version.is(filter.getVersion()));
        }

        EmpireUtils.addOrderBy(cmd, filter.getOrderBy(), filter.getOrderSort(), table.id, true);
        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());
            if (!reader.skipRows(filter.getOffset())) {
                return Collections.emptyList();
            }
            ArrayList<Version> versions = new ArrayList<Version>(filter.getLimit());
            for (Iterator<DBRecordData> it = reader.iterator(filter.getLimit()); it.hasNext();) {
                DBRecordData rec = it.next();
                Version version = new Version();
                rec.setBeanProperties(version);

                versions.add(version);
            }
            return versions;
        } finally {
            reader.close();
        }
    }

    @Override
    public int countByFilter(VersionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.count());
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
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
        if (filter.getVersion() != null) {
            cmd.where(table.version.is(filter.getVersion()));
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
    public void deleteById(Integer versionId) {
        DBCommand cmd = database.createCommand();
        cmd.where(table.id.is(versionId));
        database.executeDelete(table, cmd, getConnection());
    }

    private Optional<Version> getBeanProperties(DBRecordData record, Version existingVersion) {
        Version targetVersion = Optional.ofNullable(existingVersion).orElseGet(Version::new);
        record.setBeanProperties(targetVersion);
        return Optional.of(targetVersion);
    }
}
