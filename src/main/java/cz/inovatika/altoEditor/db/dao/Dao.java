package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.DataSource;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.utils.Utils.readFile;

public class Dao {

    protected static final Logger LOGGER = LogManager.getLogger(Dao.class.getName());

    public boolean createSchema() throws SQLException, IOException {
        boolean success = false;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            connection.setAutoCommit(true);

            statement = connection.createStatement();
            InputStream stream = readFile(Const.DEFAULT_RESOURCE_SQL);
            String fileContent = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

            for (String line : fileContent.split("(?m);$")) {
                statement.addBatch(line);
            }

            statement.executeBatch();

            success = true;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
        return success;
    }

    protected static int getNewId(String sequence) throws SQLException {
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

    protected static String getOrderBy(String orderBy) {
        return orderBy != null ? orderBy : "id";
    }

    protected static String getOrderByVersion(String orderBy) {
        return orderBy != null ? orderBy : "version";
    }

    protected static String getLimit(Integer limit) {
        return (limit == null || limit < 0) ? String.valueOf(Const.DEFAULT_SQL_LIMIT_SIZE) : String.valueOf(limit);
    }

    protected static String getOffset(Integer offset) {
        return (offset == null || offset < 0) ? "0" : String.valueOf(offset);
    }

    protected static String getDefaultOrderBy(String orderBy) {
        return orderBy != null ? ", id" : "";
    }

    protected static String getOrderSort(String orderSort) {
        return orderSort != null ? orderSort : "asc";
    }

    protected static String getOrderSortInverse(String orderSort) {
        return orderSort != null ? orderSort : "desc";
    }
}
