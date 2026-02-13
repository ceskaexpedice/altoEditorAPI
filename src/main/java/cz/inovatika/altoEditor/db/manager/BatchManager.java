package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.filter.BatchFilter;
import cz.inovatika.altoEditor.db.model.Batch;
import cz.inovatika.altoEditor.utils.Const;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The BatchManager class is responsible for managing Batch entities, including creation,
 * updating, and querying batches. It acts as a service layer to interact with the underlying
 * data access layer, ensuring transactional integrity and logging.
 *
 * This class implements a singleton design pattern with a required initialization method to
 * provide an instance of {@code DaoFactory}, facilitating the creation and management of
 * DAOs and transactions.
 */
public class BatchManager {

    private static final Logger LOG = LogManager.getLogger(BatchManager.class.getName());
    private static BatchManager INSTANCE;

    private final DaoFactory daos;

    public static void setInstance(DaoFactory daos) {
        INSTANCE = new BatchManager(daos);
    }

    public static BatchManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("set instance first!");
        }
        return INSTANCE;
    }

    public BatchManager(DaoFactory daos) {
        if (daos == null) {
            throw new NullPointerException("daos");
        }
        this.daos = daos;
    }

    public Batch addNewBatch(String pid, String priority, String instanceId, Integer ocrEngine) {
        Batch batch = new Batch();
        batch.setPid(pid);
        batch.setPriority(priority);
        batch.setInstance(instanceId);
        batch.setOcrEngine(ocrEngine);
        batch.setState(Const.BATCH_STATE_PLANNED);

        return updateBatch(batch);
    }

    private Batch updateBatch(Batch batch) {
        Objects.requireNonNull(batch, "batch must not be null");

        BatchDao batchDao = daos.createBatchDao();
        Transaction tx = daos.createTransaction();

        batchDao.setTransaction(tx);

        try {
            LOG.info("Batch " + batch.getId() + " state " + batch.getState() + " - " + batch.getLog());
            batchDao.update(batch);
            tx.commit();
            return batchDao.findById(batch.getId()).orElseThrow(() -> new IllegalStateException("Batch not found after update, id=" + batch.getId()));
        } catch (Throwable t) {
            tx.rollback();
            LOG.error("Failed to update batch: " + batch, t);
            throw new IllegalStateException(String.valueOf(batch), t);
        } finally {
            tx.close();
        }
    }

    public List<Batch> findBatch(BatchFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        BatchDao dao = daos.createBatchDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.findByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public int getBatchesCount(BatchFilter filter) {
        Objects.requireNonNull(filter, "filter must not be null");

        BatchDao dao = daos.createBatchDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            return dao.countByFilter(filter);
        } finally {
            tx.close();
        }
    }

    public Batch startWaitingBatch(Batch batch) {
        Objects.requireNonNull(batch, "batch must not be null");
        batch.setState(Const.BATCH_STATE_RUNNING);

        return updateBatch(batch);
    }

    public Batch setSubStateBatch(Batch batch, String subState) {
        Objects.requireNonNull(batch, "batch must not be null");
        batch.setSubState(subState);

        return updateBatch(batch);
    }

    public Batch updateInfoBatch(Batch batch, File folder) {
        if (folder == null || !folder.isDirectory()) {
            throw new IllegalArgumentException("folder must be a valid directory");
        }
        File[] files = folder.listFiles();
        int estimateItemNumber = files != null ? files.length : 0;
        String type = estimateItemNumber == 1 ? Const.BATCH_TYPE_SINGLE : Const.BATCH_TYPE_MULTIPLE;
        batch.setEstimateItemNumber(estimateItemNumber);
        batch.setType(type);

        return updateBatch(batch);
    }

    public Batch finishedWithError(Batch batch, Throwable t) {
        Objects.requireNonNull(batch, "batch must not be null");

        batch.setState(Const.BATCH_STATE_FAILED);
        batch.setLog(toString(t));

        return updateBatch(batch);
    }

    public Batch finishedSuccesfully(Batch batch) {
        batch.setState(Const.BATCH_STATE_DONE);
        batch.setSubState(Const.BATCH_SUBSTATE_DONE);

        return updateBatch(batch);
    }

    public List<Batch> findWaitingBatches() {
        BatchFilter filter = BatchFilter.builder().state(Const.BATCH_STATE_PLANNED).build();
        return findBatch(filter);
    }

    public List<Batch> findRunningBatches() {
        BatchFilter filter = BatchFilter.builder().state(Const.BATCH_STATE_RUNNING).build();
        return findBatch(filter);
    }

    private static String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw, true));
        String exception = sw.toString();
        if (exception.length() > 240) {
            exception = exception.substring(0, 239);
        }
        return exception;
    }

    public void deleteById(Integer batchId) {
        Objects.requireNonNull(batchId, "batchId must not be null");

        BatchDao dao = daos.createBatchDao();
        Transaction tx = daos.createTransaction();

        dao.setTransaction(tx);

        try {
            dao.deleteById(batchId);
            tx.commit();
        } finally {
            tx.close();
        }
    }
}