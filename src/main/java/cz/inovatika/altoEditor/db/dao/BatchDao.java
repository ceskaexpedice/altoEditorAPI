package cz.inovatika.altoEditor.db.dao;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.db.dao.Dao.getNewId;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSort;
import static cz.inovatika.altoEditor.utils.Utils.getNextDate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class BatchDao {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BatchDao.class.getName());

    public static Batch addNewBatch(String pid, String priority, String instanceId, Integer dObjId) throws SQLException {
        int batchId = createNewBatch(pid, priority, instanceId, dObjId);
        return getBatchById(batchId);
    }

    public static Batch startWaitingBatch(Batch batch) throws SQLException {
        updateBatchState(Const.BATCH_STATE_RUNNING, batch.getId(), null);
        return getBatchById(batch.getId());
    }

    public static Batch setSubStateBatch(Batch batch, String subState) throws SQLException {
        updateBatchSubState(subState, batch.getId());
        return getBatchById(batch.getId());
    }

    public static Batch updateInfoBatch(Batch batch, File folder) throws SQLException {
        int estimateItemNumber = folder.listFiles().length;
        String type = estimateItemNumber == 1 ? Const.BATCH_TYPE_SINGLE : Const.BATCH_TYPE_MULTIPLE;
        updateBatchInfo(estimateItemNumber, type, batch.getId());
        return getBatchById(batch.getId());
    }

    public static Batch finishedWithError(Batch batch, Throwable t) throws SQLException {
        updateBatchState(Const.BATCH_STATE_FAILED, batch.getId(), toString(t));
        return getBatchById(batch.getId());
    }

    public static Batch finishedSuccesfully(Batch batch) throws SQLException {
        updateBatchState(Const.BATCH_STATE_DONE, batch.getId(), null);
        return getBatchById(batch.getId());
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

    private static Batch getBatchById(Integer batchId) throws SQLException {
        if (batchId == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from batch where id = '" + batchId + "'");
            while (resultSet.next()) {
                return new Batch(resultSet);
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }

    }

    private static int createNewBatch(String pid, String priority, String instanceId, Integer dObjId) throws SQLException {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            int batchId = getNewId("batch_id_seq");
            if (batchId > 0) {
                statement.executeUpdate("insert into batch(id, pid, instance, createdate, updatedate, state, priority, objectId) values " +
                        "(" + batchId + ", '" + pid + "' , '" + instanceId + "', NOW(), NOW(), '" + Const.BATCH_STATE_PLANNED + "', '" + priority +"', '" + dObjId +"')");
                return batchId;
            } else {
                throw new IllegalStateException("Wrong batch Id created.");
            }
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private static void updateBatchState(String state, Integer batchId, String message) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update batch set state = '" + state + "', log = '" + message + "', updatedate = NOW(), substate = null where id = '" + batchId + "'");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private static void updateBatchSubState(String subState, Integer batchId) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update batch set substate = '" + subState + "', updatedate = NOW() where id = '" + batchId + "'");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private static void updateBatchInfo(int estimateItemNumber, String type, Integer batchId) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update batch set estimateitemnumber = '" + estimateItemNumber + "', type = '" + type + "', updatedate = NOW() where id = '" + batchId + "'");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<Batch> getAllBatches(String orderBy, String orderSort) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from batch order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));
            while (resultSet.next()) {
                Batch batch = new Batch(resultSet);
                batches.add(batch);
            }
            return batches;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }
    public List<Batch> getBatches(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log, String orderBy, String orderSort) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from batch " + getQuery(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log) + " order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));
            while (resultSet.next()) {
                Batch batch = new Batch(resultSet);
                batches.add(batch);
            }
            return batches;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private String getQuery(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log) throws ParseException {
        StringBuilder queryBuilder = new StringBuilder();
        if (isBlank(id) && isBlank(pid) && isBlank(createDate) && isBlank(updateDate) && isBlank(state) && isBlank(substate) && isBlank(priority) && isBlank(type) && isBlank(instanceId) && isBlank(estimateItemNumber) && isBlank(log)) {
            return "";
        }
        queryBuilder.append("where");
        if (!isBlank(id)) {
            queryBuilder.append(" AND ").append("id = '" + id + "'");
        }
        if (!isBlank(pid)) {
            queryBuilder.append(" AND ").append("UPPER(pid) = '" + pid.toUpperCase().trim() + "'");
        }
        if (!isBlank(createDate)) {
            queryBuilder.append(" AND ").append("createDate >= '" + createDate + "' AND createDate < '" + getNextDate(createDate) + "'");
        }
        if (!isBlank(updateDate)) {
            queryBuilder.append(" AND ").append("updateDate >= '" + updateDate + "' AND updateDate < '" + getNextDate(updateDate) + "'");
        }
        if (!isBlank(state)) {
            queryBuilder.append(" AND ").append("UPPER(state) = '" + state.toUpperCase().trim() + "'");
        }
        if (!isBlank(substate)) {
            queryBuilder.append(" AND ").append("UPPER(substate) = '" + substate.toUpperCase().trim() + "'");
        }
        if (!isBlank(priority)) {
            queryBuilder.append(" AND ").append("UPPER(priority) = '" + priority.toUpperCase().trim() + "'");
        }
        if (!isBlank(type)) {
            queryBuilder.append(" AND ").append("UPPER(type) = '" + type.toUpperCase().trim() + "'");
        }
        if (!isBlank(instanceId)) {
            queryBuilder.append(" AND ").append("UPPER(instanceId) = '" + instanceId.toUpperCase().trim() + "'");
        }
        if (!isBlank(estimateItemNumber)) {
            queryBuilder.append(" AND ").append("estimateItemNumber = '" + estimateItemNumber.toUpperCase().trim() + "'");
        }
        if (!isBlank(log)) {
            queryBuilder.append(" AND ").append("log LIKE '%" + priority.trim() + "%'");
        }
        String query = queryBuilder.toString();
        return query.replace("where AND ", "where ");
    }

    public List<Batch> findWaitingBatches() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from batch where state= '" + Const.BATCH_STATE_PLANNED + "' order by id asc");
            while (resultSet.next()) {
                Batch batch = new Batch(resultSet);
                batches.add(batch);
            }
            return batches;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<Batch> findRunningBatches() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from batch where state= '" + Const.BATCH_STATE_RUNNING + "' order by id asc");
            while (resultSet.next()) {
                Batch batch = new Batch(resultSet);
                batches.add(batch);
            }
            return batches;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }
}
