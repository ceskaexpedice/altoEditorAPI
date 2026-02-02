package cz.inovatika.altoEditor.db.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a user with basic attributes such as an ID and a login.
 *
 * This class provides accessor methods to retrieve the ID and login information
 * associated with a user. It serves as a fundamental data structure for managing
 * and handling user-related information within the system.
 */
public class User {

    protected static final Logger LOGGER = LogManager.getLogger(User.class.getName());

    private Integer id = null;
    private String login = null;

    public User() {
    }

    public Integer getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
