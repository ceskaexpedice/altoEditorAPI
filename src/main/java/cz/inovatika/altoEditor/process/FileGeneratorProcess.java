package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.utils.FileUtils.deleteFolder;

public class FileGeneratorProcess implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(FileGeneratorProcess.class.getName());

    private Batch batch = null;
    private UserProfile userProfile = null;

    public FileGeneratorProcess(Batch batch, UserProfile userProfile) {
        this.batch = batch;
        this.userProfile = userProfile;
    }

    public Batch getBatch() {
        return batch;
    }

    public static void stopRunningBatches() throws SQLException {
        Manager manager = new Manager();
        List<Batch> runningBatches = manager.findRunningBatches();
        for (Batch batch : runningBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
    }

    public static void stopAllBatches() throws SQLException {
        Manager manager = new Manager();
        List<Batch> runningBatches = manager.findRunningBatches();
        for (Batch batch : runningBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
        List<Batch> waitingBatches = manager.findWaitingBatches();
        for (Batch batch : waitingBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
    }

//    public static void resumeAll(ProcessDispatcher dispatcher) throws SQLException {
//        Manager manager = new Manager();
//        List<Batch> batches2schedule = manager.findWaitingBatches();
//        for (Batch batch : batches2schedule) {
//            try {
//                FileGeneratorProcess resume = FileGeneratorProcess.resume(batch);
//                dispatcher.addPeroProcess(resume);
//            } catch (Exception ex) {
//
//            }
//        }
//    }

    private static FileGeneratorProcess resume(Batch batch, UserProfile userProfile) throws SQLException {
        FileGeneratorProcess process = FileGeneratorProcess.prepare(batch, userProfile);
        return process;
    }

    public static FileGeneratorProcess prepare(Batch batch, UserProfile userProfile) {
        FileGeneratorProcess process = new FileGeneratorProcess(batch, userProfile);
        return process;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (SQLException ex) {
            LOGGER.error("Batch " + this.getBatch().getId() + ": " + ex.getMessage());
        }
    }

    private Batch start() throws SQLException {
        if (batch == null) {
            throw new IllegalStateException("Batch is null");
        }
        if (userProfile == null) {
            throw new IllegalStateException("UserProfile is null");
        }
        try {
            batch = Manager.startWaitingBatch(batch);

            K7Downloader downloader = new K7Downloader();
            batch = Manager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_DOWNLOADING);
            File folder = downloader.saveImage(batch.getPid(), batch.getInstance(), userProfile);
            batch = Manager.updateInfoBatch(batch, folder);
            PeroOperator operator = new PeroOperator();
            batch = Manager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_GENERATING);
            PeroOperator.Result result = operator.generate(folder);
            if (result.getException() != null) {
                batch = Manager.finishedWithError(batch, result.getException());
                throw result.getException();
            }
            if (Const.BATCH_TYPE_SINGLE.equals(batch.getType())) {
                File altoFile = getAltoFile(folder);
                if (altoFile != null) {
                    batch = Manager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_SAVING);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraStorage.AkubraObject akubraObject = storage.find(batch.getPid());
                    AltoDatastreamEditor.importAlto(akubraObject, altoFile.toURI(), "ALTO updated by PERO.", AltoDatastreamEditor.ALTO_ID + ".1");
                    akubraObject.flush();
                    deleteFolder(folder);
                    if (batch.getObjectId() == null || batch.getObjectId() == 0) {
                        UserProfile tmpUser = new UserProfile(Const.USER_PERO, userProfile.getToken());
                        Manager.createDigitalObject(tmpUser, batch.getPid(), AltoDatastreamEditor.ALTO_ID + ".1", batch.getInstance(), Const.DIGITAL_OBJECT_STATE_GENERATED);
                    } else {
                        Manager.updateDigitalObjectWithState(batch.getObjectId(), Const.DIGITAL_OBJECT_STATE_GENERATED);
                    }
                    batch = Manager.finishedSuccesfully(batch);
                } else {
                    batch = Manager.finishedWithError(batch, new Exception("Alto file is missing!"));
                }
            } else {
                Manager.finishedSuccesfully(batch);
            }
            return batch;
        } catch (Throwable t) {
            t.printStackTrace();
            return Manager.finishedWithError(batch, t);
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
