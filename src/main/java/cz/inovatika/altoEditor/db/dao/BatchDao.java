package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import cz.inovatika.utils.db.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.db.dao.Dao.getOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSort;
import static cz.inovatika.altoEditor.utils.Utils.getNextDate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class BatchDao {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BatchDao.class.getName());

    public static Batch addNewBatch(String path, String priority, int estimateImageCount) throws SQLException {
        int batchId = createNewBatch(path, priority, estimateImageCount);
        return getBatchById(batchId);
    }

    public static Batch startWaitingBatch(Batch batch) throws SQLException {
        updateBatchState(Const.BATCH_STATE_RUNNING, batch.getId(), null);
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

    private static int createNewBatch(String path, String priority, int estimateImageCount) throws SQLException {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            int batchId = getNewId("batch_id_seq");
            if (batchId > 0) {
                statement.executeUpdate("insert into batch(id, folder, datum, \"create\", state, estimateitemnumber, priority, log) values (" + batchId + ", '" + path + "' , NOW(), NOW(), '" + Const.BATCH_STATE_PLANNED + "', '" + estimateImageCount + "', '" + priority + "', '')");
                return batchId;
            } else {
                throw new IllegalStateException("Wrong batch Id created.");
            }
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private static int getNewId(String sequence) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select (NEXTVAL('" + sequence + "')) as newId");
            while (resultSet.next()) {
                return resultSet.getInt(resultSet.findColumn("newId"));
            }
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
        return 0;
    }

    private static void updateBatchState(String state, Integer batchId, String message) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update batch set state = '" + state + "', log = '" + message + "', datum = NOW() where id = '" + batchId + "'");
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
    public List<Batch> getBatches(String id, String folder, String create, String datum, String state, String priority, String orderBy, String orderSort) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        List<Batch> batches = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from batch " + getQuery(id, folder, create, datum, state, priority) + " order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));
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

    private String getQuery(String id, String folder, String create, String datum, String state, String priority) throws ParseException {
        StringBuilder queryBuilder = new StringBuilder();
        if (isBlank(id) && isBlank(folder) && isBlank(create) && isBlank(datum) && isBlank(state) && isBlank(priority)) {
            return "";
        }
        queryBuilder.append("where");
        if (!isBlank(id)) {
            queryBuilder.append(" AND ").append("id = '" + id + "'");
        }
        if (!isBlank(folder)) {
            queryBuilder.append(" AND ").append("UPPER(folder) LIKE '%" + folder.toUpperCase().trim() + "%'");
        }
        if (!isBlank(create)) {
            queryBuilder.append(" AND ").append("\"create\" >= '" + create + "' AND \"create\" < '" + getNextDate(create) + "'");
        }
        if (!isBlank(datum)) {
            queryBuilder.append(" AND ").append("datum >= '" + datum + "' AND datum < '" + getNextDate(datum) + "'");
        }
        if (!isBlank(state)) {
            queryBuilder.append(" AND ").append("UPPER(state) = '" + state.toUpperCase().trim() + "'");
        }
        if (!isBlank(priority)) {
            queryBuilder.append(" AND ").append("UPPER(priority) = '" + priority.toUpperCase().trim() + "'");
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
