package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.models.Batch;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class PeroProcess implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PeroProcess.class.getName());

    private Batch batch = null;

    public PeroProcess(Batch batch) {
        this.batch = batch;
    }

    public Batch getBatch() {
        return batch;
    }

    public static void stopRunningBatches() throws SQLException {
        BatchDao batchDao = new BatchDao();
        List<Batch> runningBatches = batchDao.findRunningBatches();
        for (Batch batch : runningBatches) {
            batchDao.finishedWithError(batch, new Exception("Application has been stopped."));
        }
    }

    public static void resumeAll(ProcessDispatcher dispatcher) throws SQLException {
        BatchDao batchDao = new BatchDao();
        List<Batch> batches2schedule = batchDao.findWaitingBatches();
        for (Batch batch : batches2schedule) {
            try {
                PeroProcess resume = PeroProcess.resume(batch);
                dispatcher.addPeroProcess(resume);
            } catch (Exception ex) {

            }
        }
    }

    private static PeroProcess resume(Batch batch) {
        PeroProcess process = PeroProcess.prepare(batch);
        return process;
    }

    public static PeroProcess prepare(Batch batch) {
        PeroProcess process = new PeroProcess(batch);
        return process;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (SQLException ex) {
            LOGGER.severe("Batch " + this.getBatch().getId() + ": " + ex.getMessage());
        }
    }

    private Batch start() throws SQLException {
        if (batch == null) {
            throw new IllegalStateException("Batch is null");
        }
        try {
            batch = BatchDao.startWaitingBatch(batch);
            PeroOperator operator = new PeroOperator();
            PeroOperator.Result result = operator.generate(new File(batch.getFolder()));
            if (result.getException() != null) {
                batch = BatchDao.finishedWithError(batch, result.getException());
                throw result.getException();
            } else {
                batch = BatchDao.finishedSuccesfully(batch);
            }
            return batch;
        } catch (Throwable t) {
            t.printStackTrace();
            return BatchDao.finishedWithError(batch, t);
        }
    }
}
