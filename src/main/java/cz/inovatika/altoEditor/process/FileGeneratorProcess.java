package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.db.manager.BatchManager;
import cz.inovatika.altoEditor.db.manager.DigitalObjectManager;
import cz.inovatika.altoEditor.db.manager.UserManager;
import cz.inovatika.altoEditor.db.model.Batch;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.kramerius.K7Utility;
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
        BatchManager manager = BatchManager.getInstance();
        List<Batch> runningBatches = manager.findRunningBatches();
        for (Batch batch : runningBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
    }

    public static void stopAllBatches() throws SQLException {
        BatchManager manager = BatchManager.getInstance();
        List<Batch> runningBatches = manager.findRunningBatches();
        for (Batch batch : runningBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
        List<Batch> waitingBatches = manager.findWaitingBatches();
        for (Batch batch : waitingBatches) {
            manager.finishedWithError(batch, new Exception("Application has been stopped."));
        }
    }

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
        BatchManager batchManager = BatchManager.getInstance();
        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            batch = batchManager.startWaitingBatch(batch);

            K7Utility downloader = new K7Utility();
            batch = batchManager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_DOWNLOADING);

            DigitalObject digitalObject = null;
            if (batch.getObjectId() != null && batch.getObjectId() != 0) {
                digitalObject = digitalObjectManager.getDigitalObject(batch.getObjectId());
            }

            File folder = downloader.saveImage(batch.getPid(), batch.getInstance(), userProfile, digitalObject.getModel());
            batch = batchManager.updateInfoBatch(batch, folder);
            PeroOperator operator = new PeroOperator();
            batch = batchManager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_GENERATING);
            PeroOperator.Result result = operator.generate(folder, batch.getOcrEngine());
            if (result.getException() != null) {
                batch = batchManager.finishedWithError(batch, result.getException());
                throw result.getException();
            }
            if (Const.BATCH_TYPE_SINGLE.equals(batch.getType())) {
                File altoFile = getAltoFile(folder);
                if (altoFile != null) {
                    batch = batchManager.setSubStateBatch(batch, Const.BATCH_SUBSTATE_SAVING);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraStorage.AkubraObject akubraObject = storage.find(batch.getPid());
                    AltoDatastreamEditor.importAlto(akubraObject, altoFile.toURI(), "ALTO updated by PERO.", AltoDatastreamEditor.ALTO_ID + ".1");
                    akubraObject.flush();
                    deleteFolder(folder);
                    if (batch.getObjectId() == null || batch.getObjectId() == 0) {
                        UserProfile tmpUser = new UserProfile(Const.USER_PERO, userProfile.getToken());
                        digitalObject = digitalObjectManager.addNewDigitalObject(tmpUser.getUsername(), batch.getPid(), "1", batch.getInstance(), Const.DIGITAL_OBJECT_STATE_GENERATED);
                    } else {
                        digitalObject.setState(Const.DIGITAL_OBJECT_STATE_GENERATED);
                        digitalObjectManager.updateDigitalObject(digitalObject);

                    }
                    batch = batchManager.finishedSuccesfully(batch);
                } else {
                    batch = batchManager.finishedWithError(batch, new Exception("Alto file is missing!"));
                }
            } else {
                UserManager userManager = UserManager.getInstance();
                Integer userId = userManager.getOrCreateUser(Const.USER_PERO);

                digitalObject.setrUserId(userId);
                digitalObject.setState(Const.DIGITAL_OBJECT_STATE_GENERATED);
                digitalObject.setVersion("1");

                digitalObjectManager.updateDigitalObject(digitalObject);
                batchManager.finishedSuccesfully(batch);
            }
            return batch;
        } catch (Throwable t) {
            t.printStackTrace();
            return batchManager.finishedWithError(batch, t);
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
