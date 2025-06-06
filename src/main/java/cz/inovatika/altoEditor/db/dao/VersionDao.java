package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.DataSource;
import cz.inovatika.altoEditor.db.models.Version;
import cz.inovatika.altoEditor.utils.Utils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersionDao {

    protected static final Logger LOGGER = LogManager.getLogger(VersionDao.class.getName());

    public static List<Version> getAllVersions() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<Version> versions = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from version order by datum, id desc");
            while (resultSet.next()) {
                Version version = new Version(resultSet);
                versions.add(version);
            }
            return versions;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static Version getActualVersion() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from version order by id desc limit 1");
            while (resultSet.next()) {
                return new Version(resultSet);
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }
}
