package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.utils.Const;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import static cz.inovatika.altoEditor.utils.FileUtils.deleteFolder;

public class FileGeneratorProcess implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FileGeneratorProcess.class.getName());

    private Batch batch = null;

    public FileGeneratorProcess(Batch batch) {
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
                FileGeneratorProcess resume = FileGeneratorProcess.resume(batch);
                dispatcher.addPeroProcess(resume);
            } catch (Exception ex) {

            }
        }
    }

    private static FileGeneratorProcess resume(Batch batch) {
        FileGeneratorProcess process = FileGeneratorProcess.prepare(batch);
        return process;
    }

    public static FileGeneratorProcess prepare(Batch batch) {
        FileGeneratorProcess process = new FileGeneratorProcess(batch);
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

            K7Downloader downloader = new K7Downloader();
            batch = BatchDao.setSubStateBatch(batch, Const.BATCH_SUBSTATE_DOWNLOADING);
            File folder = downloader.saveImage(batch.getPid(), batch.getInstance());
            batch = BatchDao.updateInfoBatch(batch, folder);
            PeroOperator operator = new PeroOperator();
            batch = BatchDao.setSubStateBatch(batch, Const.BATCH_SUBSTATE_GENERATING);
            PeroOperator.Result result = operator.generate(folder);
            if (result.getException() != null) {
                batch = BatchDao.finishedWithError(batch, result.getException());
                throw result.getException();
            }
            if (Const.BATCH_TYPE_SINGLE.equals(batch.getType())) {
                File altoFile = getAltoFile(folder);
                if (altoFile != null) {
                    batch = BatchDao.setSubStateBatch(batch, Const.BATCH_SUBSTATE_SAVING);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraStorage.AkubraObject akubraObject = storage.find(batch.getPid());
                    AltoDatastreamEditor.importAlto(akubraObject, altoFile.toURI(), "ALTO updated by PERO.", AltoDatastreamEditor.ALTO_ID + ".1");
                    akubraObject.flush();
                    deleteFolder(folder);
                    DigitalObjectDao doDao = new DigitalObjectDao();
                    if (batch.getObjectId() == null || batch.getObjectId() == 0) {
                        doDao.createDigitalObject("PERO", batch.getPid(), AltoDatastreamEditor.ALTO_ID + ".1", batch.getInstance(), Const.DIGITAL_OBJECT_STATE_GENERATED);
                    } else {
                        doDao.updateDigitalObjectWithState(batch.getObjectId(), Const.DIGITAL_OBJECT_STATE_GENERATED);
                    }
                    batch = BatchDao.finishedSuccesfully(batch);
                } else {
                    batch = BatchDao.finishedWithError(batch, new Exception("Alto file is missing!"));
                }
            } else {
                BatchDao.finishedSuccesfully(batch);
            }
            return batch;
        } catch (Throwable t) {
            t.printStackTrace();
            return BatchDao.finishedWithError(batch, t);
        }
    }

    private File getAltoFile(File folder) {
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".xml")) {
                return file;
            }
        }
        return null;
    }
}
