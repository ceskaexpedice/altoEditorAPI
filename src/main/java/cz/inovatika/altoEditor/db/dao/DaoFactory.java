package cz.inovatika.altoEditor.db.dao;

/**
 * Factory interface for creating instances of various Data Access Objects (DAO) and managing transactions.
 * This factory is intended to provide an abstraction layer for DAO creation, ensuring that the
 * necessary DAOs and transactions are instantiated and managed properly.
 */
public interface DaoFactory {

    Transaction createTransaction();

    BatchDao createBatchDao();

    DigitalObjectDao createDigitalObjectDao();

    UserDao createUserDao();

    VersionDao createVersionDao();

    void init();

}
