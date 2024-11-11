package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.utils.Utils;
import cz.inovatika.utils.db.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserDao {

    protected static final Logger LOGGER = LogManager.getLogger(UserDao.class.getName());

    public static List<User> getAllUsers() throws SQLException {
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

    public static User getUserByLogin(String login) throws SQLException {
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

    public static User getUserById(String userId) throws SQLException {
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

    public static void createUser(String login) throws SQLException {
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

    public static void updateUser(String userId, String login) throws SQLException {
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

}
