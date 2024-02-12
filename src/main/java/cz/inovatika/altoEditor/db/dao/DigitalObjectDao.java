package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.models.DigitalObject;
import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.Utils;
import cz.inovatika.utils.db.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.db.dao.Dao.getOrderBy;
import static cz.inovatika.altoEditor.db.dao.Dao.getOrderSort;

public class DigitalObjectDao {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DigitalObjectDao.class.getName());

    public List<DigitalObjectView> getAllDigitalObjects(String orderBy, String orderSort) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        List<DigitalObjectView> digitalObjects = new ArrayList();
        try {
            connection = DataSource.getConnection();
            statement = connection.createStatement();

            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));
            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }

    public List<DigitalObjectView> getDigitalObjects(String login, String pid) throws SQLException {
        return getDigitalObjects(login, pid, null, null);
    }

    public List<DigitalObjectView> getDigitalObjects(String login, String pid, String orderBy, String orderSort) throws SQLException {
        UserDao userDao = new UserDao();
        if (login != null && !login.isEmpty() && pid != null && !pid.isEmpty()) {
            User user = userDao.getUserByLogin(login);
            if (user == null || user.getId() == null) {
//                throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                userDao.createUser(login);
                user = userDao.getUserByLogin(login);
                if (user == null || user.getId() == null) {
                    throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                }
            }
            return getDigitalObjectsByUserIdAndPid(user.getId(), pid, orderBy, orderSort);
        } else if (login != null && !login.isEmpty()) {
            User user = userDao.getUserByLogin(login);
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
            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where pid = '" + pid + "' and version = (\n" +
                    "select max(version) from digitalobject where pid = '" + pid + "')");

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId()))));
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
            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where pid = '" + pid + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
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

    public void createDigitalObject(String login, String pid, String version, String instanceId) throws SQLException {
        createDigitalObject(login, pid, version, instanceId, Const.DIGITAL_OBJECT_STATE_NEW);
    }

    public void createDigitalObject(String login, String pid, String versionXml, String instanceId, String state) throws SQLException {
        if (login == null || pid == null || versionXml == null) {
            return;
        }
        UserDao userDao = new UserDao();
        String versionId = versionXml.substring(versionXml.indexOf(".") + 1);
        User user = userDao.getUserByLogin(login);
        if (user == null || user.getId() == null) {
            userDao.createUser(login);
            user = userDao.getUserByLogin(login);
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
            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' and pid = '" + pid + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId()))));
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
            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where id = '" + objectId + "'");

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                return new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId())));
            }
            return null;
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
            UserDao userDao = new UserDao();

            final ResultSet resultSet = statement.executeQuery("select * from digitalobject where ruserid = '" + userId + "' order by " + getOrderBy(orderBy) + " " + getOrderSort(orderSort));

            while (resultSet.next()) {
                DigitalObject digitalObject = new DigitalObject(resultSet);
                digitalObjects.add(new DigitalObjectView(digitalObject, userDao.getUserById(String.valueOf(digitalObject.getrUserId()))));
            }
            return digitalObjects;
        } finally {
            Utils.closeSilently(statement);
            Utils.closeSilently(connection);
        }
    }
}
