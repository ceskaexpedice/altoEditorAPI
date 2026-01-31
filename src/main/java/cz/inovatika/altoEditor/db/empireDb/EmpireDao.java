package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.Transaction;
import java.sql.Connection;
import java.util.Objects;

/**
 * Abstract base class for DAO (Data Access Object) implementations within the Empire database context.
 * Provides common functionality for managing database transactions and retrieving connections, ensuring
 * consistency across derived DAO classes.
 *
 * Responsibilities:
 * - Enforces the use of a valid database connection.
 * - Ensures a transaction is set prior to DAO operations.
 *
 * Typical subclasses of this class should implement data-specific operations such as creating, reading,
 * updating, or deleting records in the database.
 */
public abstract class EmpireDao {

    private static final String TRANSACTION_PARAM = "transaction";

    protected final AltoEditorDatabase database;
    protected SqlTransaction transaction;

    public EmpireDao(AltoEditorDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public final void setTransaction(SqlTransaction transaction) {
        this.transaction = Objects.requireNonNull(transaction, TRANSACTION_PARAM);
    }

    public final void setTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, TRANSACTION_PARAM);

        if (transaction instanceof SqlTransaction sqlTransaction) {
            setTransaction(sqlTransaction);
            return;
        }

        throw new IllegalArgumentException("Unsupported transaction type: " + transaction.getClass().getName());
    }

    protected final Connection getConnection() {
        if (transaction == null) {
            throw new IllegalStateException("Transaction is not set. Call setTransaction(...) before using DAO operations.");
        }
        return transaction.getConnection();
    }

}
