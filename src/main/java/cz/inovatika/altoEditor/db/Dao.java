package cz.inovatika.altoEditor.db;

import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import cz.inovatika.utils.db.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Utils.readFile;

public class Dao {

    protected static final Logger LOG = LoggerFactory.getLogger(Dao.class.getName());

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

    public List<Version> getAllVersions() throws SQLException {
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

    public Version getActualVersion() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select * from version order by datum, id desc limit 1");
            while (resultSet.next()) {
                return new Version(resultSet);
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<User> getAllUsers() throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<User> users = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from users order by login asc");
            while (resultSet.next()) {
                User user = new User(resultSet);
                users.add(user);
            }
            return users;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public User getUserByLogin(String login) throws SQLException {
        if (login == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from users where login = '" + login + "'");
            while (resultSet.next()) {
                return new User(resultSet);
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public User getUserById(String userId) throws SQLException {
        if (userId == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from users where id = '" + userId + "'");
            while (resultSet.next()) {
                return new User(resultSet);
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public void createUser(String login) throws SQLException {
        if (login == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("insert into users (id, login) values (NEXTVAL('users_id_seq'), '" + login + "')");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public void updateUser(String userId, String login) throws SQLException {
        if (userId == null || login == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update users set login = '" + login + "' where id = '" + userId + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<DigitalObjectView> getAllDigitalObjects(String orderBy, String orderSort) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));
            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    private String getOrderBy(String orderBy) {
        return orderBy != null ? orderBy : "id";
    }

    private String getOrderSort(String orderSort) {
        return orderSort != null ? orderSort : "asc";
    }

    public List<DigitalObjectView> getDigitalObjects(String login, String pid) throws SQLException {
        return getDigitalObjects(login, pid, null, null);
    }

    public List<DigitalObjectView> getDigitalObjects(String login, String pid, String orderBy, String orderSort) throws SQLException {
        if (login != null && !login.isEmpty() && pid != null && !pid.isEmpty()) {
            User user = getUserByLogin(login);
            if (user == null || user.getId() == null) {
//                throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                createUser(login);
                user = getUserByLogin(login);
                if (user == null || user.getId() == null) {
                    throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                }
            }
            return getDigitalObjectsByUserIdAndPid(user.getId(), pid, orderBy, orderSort);
        } else if (login != null && !login.isEmpty()) {
            User user = getUserByLogin(login);
            if (user == null || user.getId() == null) {
                throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
            }
            return getDigitalObjectsByUserId(user.getId(), orderBy, orderSort);
        } else if (pid != null && !pid.isEmpty()) {
            return getDigitalObjectsByPid(pid, orderBy, orderSort);
        } else {
            return null;
        }
    }

    public List<DigitalObjectView> getDigitalObjectsWithMaxVersionByPid(String pid) throws SQLException {
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
                digitalObjects.add(new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<DigitalObjectView> getDigitalObjectsByPid(String pid, String orderBy, String orderSort) throws SQLException {
        if (pid == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where pid = '" + pid + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<DigitalObjectView> getDigitalObjectsByUserId(Integer userId, String orderBy, String orderSort) throws SQLException {
        if (userId == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<DigitalObjectView> getDigitalObjectsByUserIdAndPid(Integer userId, String pid, String orderBy, String orderSort) throws SQLException {
        if (userId == null || pid == null) {
            return null;
        }
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' and pid = '" + pid + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public DigitalObjectView getDigitalObjectById(Integer objectId) throws SQLException {
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
                return new DigitalObjectView(digitalObject, getUserById(String.valueOf(digitalObject.getrUserId())));
            }
            return null;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public void createDigitalObject(String login, String pid, String version, String instanceId) throws SQLException {
        createDigitalObject(login, pid, version, instanceId, Const.DIGITAL_OBJECT_STATE_NEW);
    }

    public void createDigitalObject(String login, String pid, String versionXml, String instanceId, String state) throws SQLException {
        if (login == null || pid == null || versionXml == null) {
            return;
        }
        String versionId = versionXml.substring(versionXml.indexOf(".") + 1);
        User user = getUserByLogin(login);
        if (user == null || user.getId() == null) {
            createUser(login);
            user = getUserByLogin(login);
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("insert into digitalobject (id, ruserid, pid, version, datum, state, instance) values " +
                    "(NEXTVAL('digitalobject_id_seq'), '" + user.getId() + "', '" + pid +"', '" + versionId + "', now(), '" + state + "', '" + instanceId + "')");
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public void updateDigitalObject(Integer objectId, String versionXml) throws SQLException {
        if (objectId == null || versionXml == null) {
            return;
        }
        String versionId = versionXml.substring(versionXml.indexOf(".") + 1);
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set version = '" + versionId + "', datum = NOW() where id = '" + objectId + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public void updateDigitalObjectWithState(Integer objectId, String state) throws SQLException {
        if (objectId == null || state == null) {
            return;
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();
            statement.executeUpdate("update digitalobject set state = '" + state + "' where id = '" + objectId + "'");

        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }
}
