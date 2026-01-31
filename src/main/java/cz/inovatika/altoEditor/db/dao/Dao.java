package cz.inovatika.altoEditor.db.dao;

/**
 * Base DAO contract for participating in a database {@link Transaction}.
 * <p>
 * Implementations are expected to reject {@code null} transactions (e.g. by throwing
 * {@link NullPointerException} or {@link IllegalArgumentException}) and store the provided
 * transaction for subsequent DAO operations.
 */
public interface Dao {

    void setTransaction(Transaction transaction);
}
