package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.DataSource;
import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.models.DigitalObject;
import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.models.DigitalObjectView;
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
import static cz.inovatika.altoEditor.db.dao.Dao.getOffset;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderByVersion;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSort;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSortInverse;
import static cz.inovatika.altoEditor.utils.Utils.getNextDate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class DigitalObjectDao {

    protected static final Logger LOGGER = LogManager.getLogger(DigitalObjectDao.class.getName());

    public static List<DigitalObjectView> getAllDigitalObjects(String orderBy, String orderSort) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where not state in ('" + Const.DIGITAL_OBJECT_STATE_GENERATED + "') order by " + getOrderBy(orderBy) + " " + getOrderSortInverse(orderSort) + getDefaultOrderBy(orderBy));
            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static List<DigitalObjectView> getDigitalObjectsWithMaxVersionByPid(String pid) throws SQLException {
        if (pid == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where pid = '" + pid + "' and version = (\n" +
                    "select max(version) from digitalobject where pid = '" + pid + "')");

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static List<DigitalObjectView> getDigitalObjectsByPid(String pid, String orderBy, String orderSort) throws SQLException {
        if (pid == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where pid = '" + pid + "' order by " + getOrderByVersion(orderBy) + " " + getOrderSortInverse(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static void updateDigitalObject(Integer objectId, String versionXml) throws SQLException {
        if (objectId == null || versionXml == null) {
            return;
        }
        String versionId = versionXml.substring(versionXml.indexOf(".") + 1);
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set version = '" + versionId + "', datum = NOW(), state = '" + Const.DIGITAL_OBJECT_STATE_EDITED + "' where id = '" + objectId + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static void updateDigitalObjectWithState(Integer objectId, String state) throws SQLException {
        if (objectId == null || state == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set datum = NOW(), state = '" + state + "' where id = '" + objectId + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static void lockDigitalObject(String pid) throws SQLException {
        if (pid == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set datum = NOW(), lock = 'true' where pid = '" + pid + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static void unlockDigitalObject(String pid) throws SQLException {
        if (pid == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set datum = NOW(), lock = 'false' where pid = '" + pid + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static void createDigitalObject(String login, String pid, String label, String parentPath, String parentLabel, String versionXml, String instanceId, String state) throws SQLException {
        if (login == null || pid == null || versionXml == null) {
            return;
        }
        String versionId = versionXml.substring(versionXml.indexOf(".") + 1);
        User user = Manager.getUserByLogin(login);
        if (user == null) {
            Manager.createUser(login);
            user = Manager.getUserByLogin(login);
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("insert into digitalobject (id, ruserid, pid, label, parentPath, parentLabel, version, datum, state, instance) values " +
                    "(NEXTVAL('digitalobject_id_seq'), '" + user.getId() + "', '" + pid +"', '"+ label +"', '"+ parentPath +"', '"+ parentLabel +"', '" + versionId + "', now(), '" + state + "', '" + instanceId + "')");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static List<DigitalObjectView> getDigitalObjectsByUserIdAndPid(Integer userId, String pid, String orderBy, String orderSort) throws SQLException {
        if (userId == null || pid == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' and pid = '" + pid + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort) + getDefaultOrderBy(orderBy));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static DigitalObjectView getDigitalObjectById(Integer objectId) throws SQLException {
        if (objectId == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where id = '" + objectId + "'");

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                return new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId())));
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static List<DigitalObjectView> getDigitalObjectsByUserId(Integer userId, String orderBy, String orderSort) throws SQLException {
        if (userId == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort) + getDefaultOrderBy(orderBy));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static List<DigitalObjectView> getDigitalObjects(String id, String rUserId, String instance, String pid, String version, String datum, String state, String label, String parentLabel, String parentPath, String orderBy, String orderSort, Integer limit, Integer offset) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList<>();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from digitalobject " + getQuery(id, rUserId, instance, pid, version, datum, state, label, parentLabel, parentPath) + " order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort) + getDefaultOrderBy(orderBy) + " limit " + getLimit(limit) + " offset " + getOffset(offset));
            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, Manager.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public static Integer getDigitalObjectsCount(String id, String rUserId, String instance, String pid, String version, String datum, String state, String label, String parentLabel, String parentPath) throws SQLException, ParseException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select count(*) as pocet from digitalobject " + getQuery(id, rUserId, instance, pid, version, datum, state, label, parentLabel, parentPath));
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

    private static String getQuery(String id, String rUserId, String instance, String pid, String version, String datum, String state, String label, String parentLabel, String parentPath) throws ParseException {
        StringBuilder queryBuilder = new StringBuilder();
        if (isBlank(id) && isBlank(rUserId) && isBlank(instance) && isBlank(pid) && isBlank(version) && isBlank(datum) && isBlank(state) && isBlank(label) && isBlank(parentLabel) && isBlank(parentPath)) {
            return "";
        }
        queryBuilder.append("where");
        if (!isBlank(id)) {
            queryBuilder.append(" AND ").append("id = '" + id + "'");
        }
        if (!isBlank(rUserId)) {
            queryBuilder.append(" AND ").append("rUserId = '" + rUserId.trim() + "'");
        }
        if (!isBlank(pid)) {
            queryBuilder.append(" AND ").append("UPPER(pid) = '" + pid.toUpperCase().trim() + "'");
        }
        if (!isBlank(datum)) {
            queryBuilder.append(" AND ").append("datum >= '" + datum.trim() + "' AND datum < '" + getNextDate(datum) + "'");
        }
        if (!isBlank(state)) {
            queryBuilder.append(" AND ").append("UPPER(state) = '" + state.toUpperCase().trim() + "'");
        }
        if (!isBlank(instance)) {
            queryBuilder.append(" AND ").append("UPPER(instance) = '" + instance.toUpperCase().trim() + "'");
        }
        if (!isBlank(version)) {
            queryBuilder.append(" AND ").append("version = '" + version.trim() + "'");
        }
        if (!isBlank(label)) {
            queryBuilder.append(" AND ").append("UPPER(label) = '" + label.toUpperCase().trim() + "'");
        }
        if (!isBlank(parentLabel)) {
            queryBuilder.append(" AND ").append("UPPER(parentLabel) = '" + parentLabel.toUpperCase().trim() + "'");
        }
        if (!isBlank(parentPath)) {
            queryBuilder.append(" AND ").append("UPPER(parentPath) = '" + parentPath.toUpperCase().trim() + "'");
        }
        String query = queryBuilder.toString();
        return query.replace("where AND ", "where ");
    }

}
