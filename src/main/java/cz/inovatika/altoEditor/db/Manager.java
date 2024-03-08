package cz.inovatika.altoEditor.db;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.dao.UserDao;
import cz.inovatika.altoEditor.db.dao.VersionDao;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.db.models.Version;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.kramerius.K7ObjectInfo;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.models.ObjectInformation;
import cz.inovatika.altoEditor.utils.Const;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

public class Manager {

    private static final Logger LOGGER = Logger.getLogger(Manager.class.getName());

    public static Batch addNewBatch(String pid, String priority, String instanceId, Integer dObjId) throws SQLException {
        int batchId = BatchDao.createNewBatch(pid, priority, instanceId, dObjId);
        return BatchDao.getBatchById(batchId);
    }

    public static Batch startWaitingBatch(Batch batch) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_RUNNING, batch.getId(), null);
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch setSubStateBatch(Batch batch, String subState) throws SQLException {
        BatchDao.updateBatchSubState(subState, batch.getId());
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch updateInfoBatch(Batch batch, File folder) throws SQLException {
        int estimateItemNumber = folder.listFiles().length;
        String type = estimateItemNumber == 1 ? Const.BATCH_TYPE_SINGLE : Const.BATCH_TYPE_MULTIPLE;
        BatchDao.updateBatchInfo(estimateItemNumber, type, batch.getId());
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch finishedWithError(Batch batch, Throwable t) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_FAILED, batch.getId(), toString(t));
        return BatchDao.getBatchById(batch.getId());
    }

    public static Batch finishedSuccesfully(Batch batch) throws SQLException {
        BatchDao.updateBatchState(Const.BATCH_STATE_DONE, batch.getId(), null);
        return BatchDao.getBatchById(batch.getId());
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

    public static List<Batch> getAllBatches(String orderBy, String orderSort) throws SQLException {
        return BatchDao.getAllBatches(orderBy, orderSort);
    }

    public static List<Batch> getBatches(String id, String pid, String createDate, String updateDate, String state, String substate, String priority, String type, String instanceId, String estimateItemNumber, String log, String orderBy, String orderSort) throws SQLException, ParseException {
        return BatchDao.getBatches(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log, orderBy, orderSort);
    }

    public static List<Batch> findWaitingBatches() throws SQLException {
        return BatchDao.findWaitingBatches();
    }

    public static List<Batch> findRunningBatches() throws SQLException {
        return BatchDao.findRunningBatches();
    }

    public static List<DigitalObjectView> getAllDigitalObjects(String orderBy, String orderSort) throws SQLException {
        return DigitalObjectDao.getAllDigitalObjects(orderBy, orderSort);
    }

    public static List<DigitalObjectView> getDigitalObjects(String login, String pid) throws SQLException {
        return getDigitalObjects(login, pid, null, null);
    }

    public static List<DigitalObjectView> getDigitalObjectsWithMaxVersionByPid(String pid) throws SQLException {
        return DigitalObjectDao.getDigitalObjectsWithMaxVersionByPid(pid);
    }

    public static List<DigitalObjectView> getDigitalObjectsByPid(String pid, String orderBy, String orderSort) throws SQLException {
        return DigitalObjectDao.getDigitalObjectsByPid(pid, orderBy, orderSort);
    }

    public static List<DigitalObjectView> getDigitalObjects(String login, String pid, String orderBy, String orderSort) throws SQLException {
        if (login != null && !login.isEmpty() && pid != null && !pid.isEmpty()) {
            User user = Manager.getUserByLogin(login);
            if (user == null || user.getId() == null) {
//                throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                Manager.createUser(login);
                user = Manager.getUserByLogin(login);
                if (user == null || user.getId() == null) {
                    throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
                }
            }
            return getDigitalObjectsByUserIdAndPid(user.getId(), pid, orderBy, orderSort);
        } else if (login != null && !login.isEmpty()) {
            User user = Manager.getUserByLogin(login);
            if (user == null || user.getId() == null) {
                throw new IllegalStateException(String.format("User with login \"%s\" does not exists.", login));
            }
            return getDigitalObjectsByUserId(user.getId(), orderBy, orderSort);
        } else if (pid != null && !pid.isEmpty()) {
            return getDigitalObjectsByPid(pid, orderBy, orderSort);
        } else {
            return null;
        }
    }

    public static void updateDigitalObject(Integer objectId, String versionXml) throws SQLException {
        DigitalObjectDao.updateDigitalObject(objectId, versionXml);
    }

    public static void updateDigitalObjectWithState(Integer objectId, String state) throws SQLException {
        DigitalObjectDao.updateDigitalObjectWithState(objectId, state);
    }

    public static void updateDigitalObjectWithStateUploaded(Integer objectId, String state) throws SQLException {
        DigitalObjectDao.updateDigitalObjectWithState(objectId, state);
    }

    public static void updateDigitalObjectWithStateUploaded(DigitalObjectView digitalObject) throws SQLException {
        DigitalObjectDao.updateDigitalObjectWithState(digitalObject.getId(), Const.DIGITAL_OBJECT_STATE_UPLOADED);
        DigitalObjectDao.lockDigitalObject(digitalObject.getPid());
    }

    public static void lockDigitalObject(String pid) throws SQLException {
        DigitalObjectDao.lockDigitalObject(pid);
    }

    public static void unlockDigitalObject(String pid) throws SQLException {
        DigitalObjectDao.unlockDigitalObject(pid);
    }

    public static void createDigitalObject(String login, String pid, String version, String instanceId) throws SQLException, AltoEditorException, IOException {
        createDigitalObject(login, pid, null, null, null, version, instanceId);
    }

    public static void createDigitalObject(String login, String pid, String label, String parentPid, String parentLabel, String version, String instanceId) throws SQLException, AltoEditorException, IOException {
        createDigitalObject(login, pid, label, parentPid, parentLabel, version, instanceId, Const.DIGITAL_OBJECT_STATE_NEW);
    }

    public static void createDigitalObject(String login, String pid, String versionXml, String instanceId, String state) throws SQLException, AltoEditorException, IOException {
        createDigitalObject(login, pid, null, null, null, versionXml, instanceId, state);
    }

    public static void createDigitalObject(String login, String pid, String label, String parentPid, String parentLabel, String versionXml, String instanceId, String state) throws SQLException, AltoEditorException, IOException {
        if (label == null || parentPid == null || parentLabel == null) {
            if (versionXml != null && !(AltoDatastreamEditor.ALTO_ID + ".0").equals(versionXml)) {
                List<DigitalObjectView> objects = getDigitalObjects(Const.USER_ALTOEDITOR, pid);
                if (!objects.isEmpty()) {
                    DigitalObjectView object = objects.get(0);
                    if (object.getLabel() != null) {
                        DigitalObjectDao.createDigitalObject(login, pid, object.getLabel(), object.getParentPath(), object.getParentLabel(), versionXml, instanceId, state);
                        return;
                    }
                }
            }

            K7ObjectInfo objectInfo = new K7ObjectInfo();
            ObjectInformation information = objectInfo.getObjectInformation(pid, instanceId);
            DigitalObjectDao.createDigitalObject(login, pid, information.getLabel(), information.getParentPath(), information.getParentLabel(), versionXml, instanceId, state);
            return;
        }
        DigitalObjectDao.createDigitalObject(login, pid, label, parentPid, parentLabel, versionXml, instanceId, state);

    }

    public static List<DigitalObjectView> getDigitalObjectsByUserIdAndPid(Integer userId, String pid, String orderBy, String orderSort) throws SQLException {
        return DigitalObjectDao.getDigitalObjectsByUserIdAndPid(userId, pid, orderBy, orderSort);
    }

    public static DigitalObjectView getDigitalObjectById(Integer objectId) throws SQLException {
        return DigitalObjectDao.getDigitalObjectById(objectId);
    }

    public static List<DigitalObjectView> getDigitalObjectsByUserId(Integer userId, String orderBy, String orderSort) throws SQLException {
        return DigitalObjectDao.getDigitalObjectsByUserId(userId, orderBy, orderSort);
    }

    public static List<User> getAllUsers() throws SQLException {
        return UserDao.getAllUsers();
    }

    public static User getUserByLogin(String login) throws SQLException {
        return UserDao.getUserByLogin(login);
    }

    public static User getUserById(String userId) throws SQLException {
        return UserDao.getUserById(userId);
    }

    public static void createUser(String login) throws SQLException {
        UserDao.createUser(login);
    }

    public static void updateUser(String userId, String login) throws SQLException {
        UserDao.updateUser(userId, login);
    }

    public static List<Version> getAllVersions() throws SQLException {
        return VersionDao.getAllVersions();
    }

    public static Version getActualVersion() throws SQLException {
        return VersionDao.getActualVersion();
    }

}
