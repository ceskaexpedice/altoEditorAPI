package cz.inovatika.altoEditor.db;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import cz.inovatika.utils.db.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Manager.class.getName());

    public static Batch addNewBatch(String pid, String priority, String instanceId, Integer dObjId) throws SQLException {
        int batchId = BatchDao.createNewBatch(pid, priority, instanceId, dObjId);
        return BatchDao.getBatchById(batchId);
    }

    public static Batch startWaitingBatch(Batch batch) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_RUNNING, batch.getId(), null);
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch setSubStateBatch(Batch batch, String subState) throws SQLException {
        BatchDao.updateBatchSubState(subState, batch.getId());
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch updateInfoBatch(Batch batch, File folder) throws SQLException {
        int estimateItemNumber = folder.listFiles().length;
        String type = estimateItemNumber == 1 ? Const.BATCH_TYPE_SINGLE : Const.BATCH_TYPE_MULTIPLE;
        BatchDao.updateBatchInfo(estimateItemNumber, type, batch.getId());
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch finishedWithError(Batch batch, Throwable t) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_FAILED, batch.getId(), toString(t));
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch finishedSuccesfully(Batch batch) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_DONE, batch.getId(), null);
        return BatchDao.getBatchById(batch.getId());
    }

    private static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        String exception = sw.toString();
        if (exception.length() > 240) {
            exception = exception.substring(0, 239);
        }
        return exception;
    }

    public static List<Batch> getAllBatches(String orderBy, String orderSort) throws SQLException {
        return BatchDao.getAllBatches(orderBy, orderSort);
    }

    public static List<Batch> getBatches(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log, String orderBy, String orderSort) throws SQLException, ParseException {
        return BatchDao.getBatches(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log, orderBy, orderSort);
    }

    public static List<Batch> findWaitingBatches() throws SQLException {
        return BatchDao.findWaitingBatches();
    }

    public static List<Batch> findRunningBatches() throws SQLException {
        return BatchDao.findRunningBatches();
    }

}
