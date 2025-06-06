package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.DataSource;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.db.dao.Dao.getDefaultOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getLimit;
import static cz.inovatika.altoEditor.db.dao.Dao.getNewId;
import static cz.inovatika.altoEditor.db.dao.Dao.getOffset;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSort;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSortInverse;
import static cz.inovatika.altoEditor.utils.Utils.getNextDate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class BatchDao {

    protected static final Logger LOGGER = LogManager.getLogger(BatchDao.class.getName());

    public static Batch getBatchById(Integer batchId) throws SQLException {
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

    public static int createNewBatch(String pid, String priority, String instanceId, Integer dObjId) throws SQLException {
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

    public static void updateBatchState(String state, Integer batchId, String message) throws SQLException {
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

    public static void updateBatchSubState(String subState, Integer batchId) throws SQLException {
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

    public static void updateBatchInfo(int estimateItemNumber, String type, Integer batchId) throws SQLException {
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

    public static List<Batch> getAllBatches(String orderBy, String orderSort) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from batch order by " + getOrderBy(orderBy) + " " + getOrderSortInverse(orderSort) + getDefaultOrderBy(orderBy));
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

    public static List<Batch> getBatches(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log, String orderBy, String orderSort, Integer limit, Integer offset) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from batch " + getQuery(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log) + " order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort) + getDefaultOrderBy(orderBy) + " limit " + getLimit(limit) + " offset " + getOffset(offset));
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

    public static Integer getBatchesCount(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select count(*) as pocet from batch " + getQuery(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log));
            while (resultSet.next()) {
                Integer value = resultSet.getInt("pocet");
                return value;
            }
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
        return 0;
    }

    private static String getQuery(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log) throws ParseException {
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
            queryBuilder.append(" AND ").append("UPPER(instance) = '" + instanceId.toUpperCase().trim() + "'");
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

    public static List<Batch> findWaitingBatches() throws SQLException {
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

    public static List<Batch> findRunningBatches() throws SQLException {
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
