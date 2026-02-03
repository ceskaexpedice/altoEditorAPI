package cz.inovatika.altoEditor.db.manager;


import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.filter.DigitalObjectFilter;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DigitalObjectManager {

    private static final Logger LOG = LogManager.getLogger(DigitalObjectManager.class.getName());
    private static DigitalObjectManager INSTANCE;

    private final DaoFactory daos;

    public static void setInstance(DaoFactory daos) {
        INSTANCE = new DigitalObjectManager(daos);
    }

    public static DigitalObjectManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("set instance first!");
        }
        return INSTANCE;
    }

    public DigitalObjectManager(DaoFactory daos) {
        if (daos == null) {
            throw new NullPointerException("daos");
        }
        this.daos = daos;
    }

    public DigitalObject addNewDigitalObject(String login, String pid, String version, String instance) {
        return addNewDigitalObject(login, pid, version, instance, null);
    }

    public DigitalObject addNewDigitalObject(String login, String pid, String version, String instance, String state) {

        UserManager userManager = UserManager.getInstance();
        Integer userId = userManager.getOrCreateUser(login);

        DigitalObject digitalObject = new DigitalObject();
        digitalObject.setPid(pid);
        digitalObject.setVersion(version);
        digitalObject.setrUserId(userId);
        digitalObject.setInstance(instance);

        digitalObject.setState(state == null ? Const.DIGITAL_OBJECT_STATE_NEW : state);

        return updateDigitalObject(digitalObject);
    }

    public DigitalObject updateDigitalObject(DigitalObject digitalObject) {
        Objects.requireNonNull(digitalObject, "digital object must not be null");

        DigitalObjectDao digitalObjectDao = daos.createDigitalObjectDao();
        Transaction tx = daos.createTransaction();

        digitalObjectDao.setTransaction(tx);

        try {
            LOG.debug("User " + digitalObject.getId() + " pid " + digitalObject.getPid());
            digitalObjectDao.update(digitalObject);
            tx.commit();
            return digitalObjectDao.findById(digitalObject.getId()).orElseThrow(() -> new IllegalStateException("Digital Object not found after update, id=" + digitalObject.getId()));
        } catch (Throwable t) {
            tx.rollback();
            LOG.error("Failed to update digital object: " + digitalObject, t);
            throw new IllegalStateException(String.valueOf(digitalObject), t);
        } finally {
            tx.close();
        }
    }

    public DigitalObject getDigitalObject(Integer digitalObjectId) {
        Objects.requireNonNull(digitalObjectId, "digitalObjectId must not be null");

        DigitalObjectDao dao = daos.createDigitalObjectDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.findById(digitalObjectId).orElseThrow(() -> new IllegalStateException("Digital Object not found, id=" + digitalObjectId));
        } finally {
            tx.close();
        }
    }

    public List<DigitalObjectView> findDigitalObject(DigitalObjectFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DigitalObjectDao dao = daos.createDigitalObjectDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.findByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public int getDigitalObjectsCount(DigitalObjectFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        DigitalObjectDao dao = daos.createDigitalObjectDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.countByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public List<DigitalObjectView> getDigitalObjectsWithMaxVersionByPid(String pid) {
        DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid).orderBy(Const.PARAM_DIGITAL_OBJECT_VERSION_XML).orderSort("desc").build();

        List<DigitalObjectView> digitalObjectViews = findDigitalObject(filter);

        int maxVersion = 0;

        for (DigitalObjectView digitalObjectView : digitalObjectViews) {
            String version = digitalObjectView.getVersion();
            int versionInt = Integer.parseInt(version.replaceAll("[^0-9]", ""));

            if (versionInt > maxVersion) {
                maxVersion = versionInt;
            }
        }

        filter = DigitalObjectFilter.builder().pid(pid).version(String.valueOf(maxVersion)).build();

        return findDigitalObject(filter);

    }
}