package cz.inovatika.altoEditor.db.manager;


import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.dao.UserDao;
import cz.inovatika.altoEditor.db.filter.UserFilter;
import cz.inovatika.altoEditor.db.model.User;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages user operations and provides methods to interact with user data through DAOs.
 *
 * This class handles the creation, updating, and retrieval of user information in the application.
 * It relies on a DaoFactory to create and manage the required Data Access Objects and transactions
 * during database operations.
 */
public class UserManager {

    private static final Logger LOG = LogManager.getLogger(UserManager.class.getName());
    private static UserManager INSTANCE;

    private final DaoFactory daos;

    public static void setInstance(DaoFactory daos) {
        INSTANCE = new UserManager(daos);
    }

    public static UserManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("set instance first!");
        }
        return INSTANCE;
    }

    public UserManager(DaoFactory daos) {
        if (daos == null) {
            throw new NullPointerException("daos");
        }
        this.daos = daos;
    }

    public User addNewUser(String login) throws SQLException {
        User user = new User();
        user.setLogin(login);

        return updateUser(user);
    }

    public User updateUser(User user) {
        Objects.requireNonNull(user, "user must not be null");

        UserDao userDao = daos.createUserDao();
        Transaction tx = daos.createTransaction();

        userDao.setTransaction(tx);

        try {
            LOG.debug("User " + user.getId() + " login " + user.getLogin());
            userDao.update(user);
            tx.commit();
            return userDao.findById(user.getId()).orElseThrow(() -> new IllegalStateException("User not found after update, id=" + user.getId()));
        } catch (Throwable t) {
            tx.rollback();
            LOG.error("Failed to update user: " + user, t);
            throw new IllegalStateException(String.valueOf(user), t);
        } finally {
            tx.close();
        }
    }

    public List<User> findUser(UserFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        UserDao dao = daos.createUserDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.findByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public int getUsersCount(UserFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        UserDao dao = daos.createUserDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.countByFilter(filter);
        } finally {
            tx.close();
        }
    }
}