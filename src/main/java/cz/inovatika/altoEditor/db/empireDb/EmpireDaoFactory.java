package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.dao.UserDao;
import cz.inovatika.altoEditor.db.dao.VersionDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A concrete implementation of the {@code DaoFactory} interface for creating and managing
 * Data Access Objects (DAO) required by the Empire application. This factory provides
 * specialized implementations of DAOs and transaction handling for the application's
 * database interactions.
 *
 * This class is tightly coupled to the {@code EmpireConfiguration} and utilizes the
 * {@code AltoEditorDatabase} schema as the underlying data source.
 *
 * Responsibilities:
 * - Initialize the underlying database schema using the provided configuration.
 * - Provide factory methods for creating specific DAO implementations such as
 *   {@code BatchDao}, {@code DigitalObjectDao}, and {@code UserDao}.
 * - Manage the lifecycle of SQL transactions.
 */
public final class EmpireDaoFactory implements DaoFactory {

    private final AltoEditorDatabase database;
    private final EmpireConfiguration configuration;

    public EmpireDaoFactory(EmpireConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.database = Objects.requireNonNull(configuration.getSchema(), "configuration.getSchema()");
    }

    public AltoEditorDatabase getDb() {
        return database;
    }

    @Override
    public void init() {
        try {
            database.init(configuration);
        } catch (SQLException ex) {
            throw wrap(ex);
        }
    }

    @Override
    public SqlTransaction createTransaction() {
        try {
            Connection connection = configuration.getConnection();
            connection.setAutoCommit(false);
            return new SqlTransaction(connection);
        } catch (SQLException ex) {
            throw wrap(ex);
        }
    }

    @Override
    public BatchDao createBatchDao() {
        return new EmpireBatchDao(database);
    }

    @Override
    public DigitalObjectDao createDigitalObjectDao() {
        return new EmpireDigitalObjectDao(database);
    }

    @Override
    public UserDao createUserDao() {
        return new EmpireUserDao(database);
    }

    @Override
    public VersionDao createVersionDao() {
        return new EmpireVersionDao(database);
    }

    private static IllegalStateException wrap(SQLException ex) {
        return new IllegalStateException(ex);
    }
}
