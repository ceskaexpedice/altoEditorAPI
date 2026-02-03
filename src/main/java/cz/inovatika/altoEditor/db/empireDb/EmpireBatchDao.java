package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.filter.BatchFilter;
import cz.inovatika.altoEditor.db.model.Batch;
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
 * Concrete implementation of the {@link BatchDao} interface for managing {@link Batch}
 * entities using the Empire database framework. This class provides methods to create,
 * retrieve, update, and delete batch records while handling database interactions.
 *
 * This DAO operates over the {@link AltoEditorDatabase.BatchTable} and uses the Empire
 * database utilities to execute database commands and manage transactions.
 */
public class EmpireBatchDao extends EmpireDao implements BatchDao {

    protected static final Logger LOGGER = LogManager.getLogger(EmpireBatchDao.class.getName());
    private final AltoEditorDatabase.BatchTable table;

    public EmpireBatchDao(AltoEditorDatabase db) {
        super(db);
        table = db.tableBatch;
    }

    @Override
    public Batch createBatch() {
        return new Batch();
    }

    @Override
    public void update(Batch batch) throws ConcurrentModificationException {
        DBRecord record = new DBRecord();
        Connection connection = getConnection();

        if (batch.getId() == null) {
            record.create(table);
            batch.setCreateDate(new Timestamp(System.currentTimeMillis()));
        } else {
            record.read(table, batch.getId(), connection);
        }
        batch.setUpdateDate(new Timestamp(System.currentTimeMillis()));

        record = setBeanProperties(batch, record);

        try {
            record.update(connection);
        } catch (RecordUpdateInvalidException e) {
            throw new ConcurrentModificationException(e);
        }
        getBeanProperties(record, batch);
    }

    @Override
    public Optional<Batch> findById(Integer batchId) {
        DBRecord record = new DBRecord();
        try {
            record.read(table, batchId, getConnection());
            return getBeanProperties(record, null);
        } catch (RecordNotFoundException ex) {
            return Optional.empty();
        } finally {
            record.close();
        }
    }

    @Override
    public List<Batch> findByFilter(BatchFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.id, table.pid, table.createDate, table.updateDate, table.state, table.subState, table.priority, table.type, table.instance, table.objectId, table.estimateItemNumber, table.log);
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
        }
        if (filter.getPid() != null) {
            cmd.where(table.pid.is(filter.getPid()));
        }
        if (filter.getState() != null) {
            cmd.where(table.state.is(filter.getState()));
        }
        if (filter.getSubState() != null) {
            cmd.where(table.subState.is(filter.getSubState()));
        }
        if (filter.getPriority() != null) {
            cmd.where(table.priority.is(filter.getPriority()));
        }
        if (filter.getType() != null) {
            cmd.where(table.type.is(filter.getType()));
        }
        if (filter.getInstance() != null) {
            cmd.where(table.instance.is(filter.getInstance()));
        }
        if (filter.getObjectId() != null) {
            cmd.where(table.objectId.is(filter.getObjectId()));
        }
        if (filter.getEstimateItemNumber() != null) {
            cmd.where(table.estimateItemNumber.is(filter.getEstimateItemNumber()));
        }
        if (filter.getLog() != null) {
            cmd.where(table.estimateItemNumber.like(filter.getLog()));
        }
        if (filter.getCreateDateFrom() != null) {
            cmd.where(table.createDate.isMoreOrEqual(filter.getCreateDateFrom()));
        }
        if (filter.getCreateDateTo() != null) {
            cmd.where(table.createDate.isLessOrEqual(filter.getCreateDateTo()));
        }
        if (filter.getUpdateDateFrom() != null) {
            cmd.where(table.updateDate.isMoreOrEqual(filter.getUpdateDateFrom()));
        }
        if (filter.getUpdateDateTo() != null) {
            cmd.where(table.updateDate.isLessOrEqual(filter.getUpdateDateTo()));
        }


        EmpireUtils.addOrderBy(cmd, filter.getOrderBy(), filter.getOrderSort(), table.id, true);
        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());
            if (!reader.skipRows(filter.getOffset())) {
                return Collections.emptyList();
            }
            ArrayList<Batch> batches = new ArrayList<Batch>(filter.getLimit());
            for (Iterator<DBRecordData> it = reader.iterator(filter.getLimit()); it.hasNext(); ) {
                DBRecordData rec = it.next();
                Batch batch = new Batch();
                rec.setBeanProperties(batch);

                batches.add(batch);
            }
            return batches;
        } finally {
            reader.close();
        }
    }

    @Override
    public int countByFilter(BatchFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.count());
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
        }
        if (filter.getPid() != null) {
            cmd.where(table.pid.is(filter.getPid()));
        }
        if (filter.getState() != null) {
            cmd.where(table.state.is(filter.getState()));
        }
        if (filter.getSubState() != null) {
            cmd.where(table.subState.is(filter.getSubState()));
        }
        if (filter.getPriority() != null) {
            cmd.where(table.priority.is(filter.getPriority()));
        }
        if (filter.getType() != null) {
            cmd.where(table.type.is(filter.getType()));
        }
        if (filter.getInstance() != null) {
            cmd.where(table.instance.is(filter.getInstance()));
        }
        if (filter.getObjectId() != null) {
            cmd.where(table.objectId.is(filter.getObjectId()));
        }
        if (filter.getEstimateItemNumber() != null) {
            cmd.where(table.estimateItemNumber.is(filter.getEstimateItemNumber()));
        }
        if (filter.getLog() != null) {
            cmd.where(table.estimateItemNumber.like(filter.getLog()));
        }
        if (filter.getCreateDateFrom() != null) {
            cmd.where(table.createDate.isMoreOrEqual(filter.getCreateDateFrom()));
        }
        if (filter.getCreateDateTo() != null) {
            cmd.where(table.createDate.isLessOrEqual(filter.getCreateDateTo()));
        }
        if (filter.getUpdateDateFrom() != null) {
            cmd.where(table.updateDate.isMoreOrEqual(filter.getUpdateDateFrom()));
        }
        if (filter.getUpdateDateTo() != null) {
            cmd.where(table.updateDate.isLessOrEqual(filter.getUpdateDateTo()));
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
    public void deleteById(Integer batchId) {
        DBCommand cmd = database.createCommand();
        cmd.where(table.id.is(batchId));
        database.executeDelete(table, cmd, getConnection());
    }

    private DBRecord setBeanProperties(Batch batch, DBRecord record) {
        if (batch != null) {
            record.setValue(table.pid, batch.getPid());
            record.setValue(table.state, batch.getState());
            record.setValue(table.subState, batch.getSubState());
            record.setValue(table.priority, batch.getPriority());
            record.setValue(table.type, batch.getType());
            record.setValue(table.instance, batch.getInstance());
            record.setValue(table.objectId, batch.getObjectId());
            record.setValue(table.estimateItemNumber, batch.getEstimateItemNumber());
            record.setValue(table.log, batch.getLog());
            return record;
        } else {
            return record;
        }
    }

    private Optional<Batch> getBeanProperties(DBRecordData record, Batch existingBatch) {
        Batch targetBatch = Optional.ofNullable(existingBatch).orElseGet(Batch::new);
        record.setBeanProperties(targetBatch);
        return Optional.of(targetBatch);
    }
}
