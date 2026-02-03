package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.dao.VersionDao;
import cz.inovatika.altoEditor.db.filter.VersionFilter;
import cz.inovatika.altoEditor.db.model.Version;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersionManager {

    private static final Logger LOG = LogManager.getLogger(VersionManager.class.getName());
    private static VersionManager INSTANCE;

    private final DaoFactory daos;

    public static void setInstance(DaoFactory daos) {
        INSTANCE = new VersionManager(daos);
    }

    public static VersionManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("set instance first!");
        }
        return INSTANCE;
    }

    public VersionManager(DaoFactory daos) {
        if (daos == null) {
            throw new NullPointerException("daos");
        }
        this.daos = daos;
    }

    public List<Version> findVersion(VersionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        VersionDao dao = daos.createVersionDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.findByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public int getVersionCount(VersionFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        VersionDao dao = daos.createVersionDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.countByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public static List<Version> getAllVersions() throws SQLException {
//        return VersionDaoOld.getAllVersions();
        return null;
    }

    public static Version getActualVersion() throws SQLException {
//        return VersionDaoOld.getActualVersion();
        return null;
    }
}