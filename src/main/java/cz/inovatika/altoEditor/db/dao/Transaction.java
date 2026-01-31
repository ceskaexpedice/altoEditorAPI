package cz.inovatika.altoEditor.db.dao;

/**
 * Represents a contract for a transactional context, providing an abstraction
 * for managing the lifecycle of transactions. Implementing classes are expected
 * to handle the underlying mechanisms of committing, rolling back, and closing
 * the transaction.
 */
public interface Transaction {

    void commit();

    void rollback();

    void close();

}
