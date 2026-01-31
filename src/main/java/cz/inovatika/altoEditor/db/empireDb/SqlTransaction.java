package cz.inovatika.altoEditor.db.empireDb;

import cz.inovatika.altoEditor.db.dao.Transaction;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a contract for a transactional context, providing an abstraction
 * for managing the lifecycle of transactions. Implementing classes are expected
 * to handle the underlying mechanisms of committing, rolling back, and closing
 * the transaction.
 */
public final class SqlTransaction implements Transaction {
    
    private final Connection c;

    public SqlTransaction(Connection c) {
        try {
            c.setAutoCommit(false);
            this.c = c;
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Connection getConnection() {
        return c;
    }

    public void commit() {
        try {
            c.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void rollback() {
        try {
            c.rollback();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void close() {
        try {
            c.close();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
