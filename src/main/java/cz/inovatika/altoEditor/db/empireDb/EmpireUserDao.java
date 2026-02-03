package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.UserDao;
import cz.inovatika.altoEditor.db.filter.UserFilter;
import cz.inovatika.altoEditor.db.model.User;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRecord;
import org.apache.empire.db.DBRecordData;
import org.apache.empire.db.exceptions.RecordNotFoundException;
import org.apache.empire.db.exceptions.RecordUpdateInvalidException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Concrete implementation of the {@link UserDao} interface for managing {@link User} entities
 * within the Empire database system using the {@link AltoEditorDatabase.UserTable}.
 *
 * This class extends the {@link EmpireDao} abstract base class, which enforces the use of a database
 * transaction and provides core functionality for working with database connections. It leverages
 * Empire Framework utilities for CRUD operations such as creating, updating, fetching, and deleting
 * user records.
 */
public class EmpireUserDao extends EmpireDao implements UserDao {

    protected static final Logger LOGGER = LogManager.getLogger(EmpireUserDao.class.getName());
    private final AltoEditorDatabase.UserTable table;

    public EmpireUserDao(AltoEditorDatabase db) {
        super(db);
        table = db.tableUser;
    }

    @Override
    public User createUser() {
        return new User();
    }

    @Override
    public void update(User user) throws ConcurrentModificationException {
        DBRecord record = new DBRecord();
        Connection connection = getConnection();

        if (user.getId() == null) {
            record.create(table);
        } else {
            record.read(table, user.getId(), connection);
        }
        record.setValue(table.login, user.getLogin());

        for (DBColumn col : table.getColumns()) {
            Object value = record.getValue(col);
            LOGGER.info("{} = {}", col.getName(), value);
        }

        try {
            record.update(connection);
        } catch (RecordUpdateInvalidException e) {
            throw new ConcurrentModificationException(e);
        }
        getBeanProperties(record, user);
    }

    @Override
    public Optional<User> findById(Integer userId) {
        DBRecord record = new DBRecord();
        try {
            record.read(table, userId, getConnection());
            return getBeanProperties(record, null);
        } catch (RecordNotFoundException ex) {
            return Optional.empty();
        } finally {
            record.close();
        }
    }

    @Override
    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.id, table.login);
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
        }
        if (filter.getLogin() != null) {
            cmd.where(table.login.is(filter.getLogin()));
        }

        EmpireUtils.addOrderBy(cmd, filter.getOrderBy(), filter.getOrderSort(), table.id, true);
        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());
            if (!reader.skipRows(filter.getOffset())) {
                return Collections.emptyList();
            }
            ArrayList<User> useres = new ArrayList<User>(filter.getLimit());
            for (Iterator<DBRecordData> it = reader.iterator(filter.getLimit()); it.hasNext();) {
                DBRecordData rec = it.next();
                User user = new User();
                rec.setBeanProperties(user);

                useres.add(user);
            }
            return useres;
        } finally {
            reader.close();
        }
    }

    @Override
    public int countByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DBCommand cmd = database.createCommand();
        cmd.select(table.count());
        if (filter.getId() != null) {
            cmd.where(table.id.is(filter.getId()));
        }
        if (filter.getLogin() != null) {
            cmd.where(table.login.is(filter.getLogin()));
        }
        DBReader reader = new DBReader();
        try {
            reader.open(cmd, getConnection());

            if (reader.moveNext()) {
                return reader.getInt(0);
            }
            return 0;
        } finally {
            reader.close();
        }
    }

    @Override
    public void deleteById(Integer userId) {
        DBCommand cmd = database.createCommand();
        cmd.where(table.id.is(userId));
        database.executeDelete(table, cmd, getConnection());
    }

    private Optional<User> getBeanProperties(DBRecordData record, User existingUser) {
        User targetUser = Optional.ofNullable(existingUser).orElseGet(User::new);
        record.setBeanProperties(targetUser);
        return Optional.of(targetUser);
    }
}
