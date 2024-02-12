package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.dao.Dao;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import cz.inovatika.altoEditor.kramerius.K7ImageViewer;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.process.PeroOperator;
import cz.inovatika.altoEditor.process.PeroProcess;
import cz.inovatika.altoEditor.process.ProcessDispatcher;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.response.AltoEditorStringRecordResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage.AkubraObject;
import cz.inovatika.altoEditor.utils.Const;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.editor.AltoDatastreamEditor.nextVersion;
import static cz.inovatika.altoEditor.utils.Utils.getOptStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.setResult;
import static cz.inovatika.altoEditor.utils.Utils.setStringResult;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class DigitalObjectResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DigitalObjectResource.class.getName());

    public static void getImage(Context context) {
        try {
            String login = getOptStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");

            DigitalObjectDao doDao = new DigitalObjectDao();
            List<DigitalObjectView> digitalObjects = doDao.getDigitalObjects(null, pid);

            String instanceId = null;
            for (DigitalObjectView digitalObject : digitalObjects) {
                if (digitalObject.getInstance() != null && !digitalObject.getInstance().isEmpty()) {
                    instanceId = digitalObject.getInstance();
                    break;
                }
            }

            if (instanceId == null) {
                instanceId = getStringRequestValue(context, "instance");
            }

            K7ImageViewer imageViewer = new K7ImageViewer();
            HttpResponse response = imageViewer.getResponse(pid, instanceId);

            if (HTTP_OK == response.getStatusLine().getStatusCode()) {
                setResult(context, response, pid);
            } else {
                setResult(context, AltoEditorResponse.asError("FAILED: Impossible to download JPG", null));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAlto(Context context) {
        try {
            String login = getOptStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");
            String versionXml = getOptStringRequestValue(context, "versionXml");

            // hledani objektu konkretniho uzivatele
            DigitalObjectDao doDao = new DigitalObjectDao();
            List<DigitalObjectView> digitalObjects = doDao.getDigitalObjects(login, pid);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (versionXml == null || versionXml.isEmpty()) {
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
                    setStringResult(context, response);
                    return;
                }
                for (DigitalObjectView digitalObject : digitalObjects) {
                    if (versionXml.equals(digitalObject.getVersionXml())) {
                        AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                        setStringResult(context, response);
                        return;
                    }
                }
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = doDao.getDigitalObjects(null, pid);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Object found in repositories using pid - return default version");
                if (versionXml == null || versionXml.isEmpty()) {
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), AltoDatastreamEditor.ALTO_ID + ".0");
                    setStringResult(context, response);
                    return;
                }
                for (DigitalObjectView digitalObject : digitalObjects) {
                    if (versionXml.equals(digitalObject.getVersionXml())) {
                        AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                        setStringResult(context, response);
                        return;
                    }
                }
            }

            // stazeni noveho objektu z krameria
            String instanceId = getStringRequestValue(context, "instance");

            K7Downloader downloader = new K7Downloader();
            downloader.downloadFoxml(pid, instanceId, login);
            doDao.createDigitalObject("AltoEditor", pid, AltoDatastreamEditor.ALTO_ID + ".0", instanceId);
            AltoEditorStringRecordResponse response = getAltoStream(pid, AltoDatastreamEditor.ALTO_ID + ".0");
            setStringResult(context, response);

        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void updateAlto(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            String pid = getStringNodeValue(node, "pid");
            String data = getStringNodeValue(node, "data");

            // hledani objektu konkretniho uzivatele
            DigitalObjectDao doDao = new DigitalObjectDao();
            List<DigitalObjectView> digitalObjects = doDao.getDigitalObjects(login, pid);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object for user");
                } else {
                    DigitalObjectView digitalObject = digitalObjects.get(0);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + login, digitalObject.getVersionXml());
                    doDao.updateDigitalObject(digitalObject.getId(), digitalObject.getVersionXml());

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                    setStringResult(context, response);
                    return;
                }
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = doDao.getDigitalObjectsWithMaxVersionByPid(pid);
            if (!digitalObjects.isEmpty()) {
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object with max version");
                } else {
                    DigitalObjectView digitalObject = digitalObjects.get(0);
                    String versionId = nextVersion(digitalObject.getVersionXml());
                    LOGGER.debug("Object found in repositories using pid - using default version");

                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + login, versionId);
                    doDao.createDigitalObject(login, pid, versionId, digitalObject.getInstance(), Const.DIGITAL_OBJECT_STATE_EDITED);

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), versionId);
                    setStringResult(context, response);
                    return;
                }
            } else {
                throw new DigitalObjectNotFoundException(pid, String.format("This pid \"%s\" not found in repository."));
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void generatePero(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, "pid");
            String priority = getOptStringNodeValue(node, "priority");
            String versionXml = "0";

            // hledani objektu konkretniho uzivatele
            DigitalObjectDao doDao = new DigitalObjectDao();
            List<DigitalObjectView> digitalObjects = doDao.getDigitalObjects("PERO", pid);

            if (priority == null) {
                priority = Const.BATCH_PRIORITY_MEDIUM;
            }

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (versionXml == null || versionXml.isEmpty()) {
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
                    setStringResult(context, response);
                    return;
                }
                for (DigitalObjectView digitalObject : digitalObjects) {
                    if (versionXml.equals(digitalObject.getVersionXml())) {
                        AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                        setStringResult(context, response);
                        return;
                    }
                }
            } else {
                String instanceId = getInstanceFromDigitalObject(pid);
                if (isBlank(instanceId)) {
                    instanceId = getStringNodeValue(node, "instance");
                }
                K7Downloader downloader = new K7Downloader();
                File parentFile = downloader.saveImage(pid, instanceId);

                Batch batch = BatchDao.addNewBatch(parentFile.getAbsolutePath(), priority, parentFile.listFiles().length);
                PeroProcess process = PeroProcess.prepare(batch);
                ProcessDispatcher.getDefault().addPeroProcess(process);
                AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
                response.setPid(pid);
                response.setContent("Proces zapsán");
                setStringResult(context, response);
            }

        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    private static String getInstanceFromDigitalObject(String pid) throws SQLException {
        DigitalObjectDao doDao = new DigitalObjectDao();
        List<DigitalObjectView> digitalObjects = doDao.getDigitalObjects(null, pid);
        if (!digitalObjects.isEmpty()) {
            for (DigitalObjectView digitalObject : digitalObjects) {
                if (!isBlank(digitalObject.getInstance())) {
                    return digitalObject.getInstance();
                }
            }
        }
        return null;
    }

    private static AltoEditorStringRecordResponse getAltoStream(@NotNull String pid, @NotNull String versionId) throws IOException, AltoEditorException {
        AkubraStorage storage = AkubraStorage.getInstance();
        AkubraObject object = storage.find(pid);
        AltoDatastreamEditor altoEditor = AltoDatastreamEditor.alto(object);
        AltoEditorStringRecordResponse response = altoEditor.readRecord(versionId);
        response.setPid(pid);
        return response;
    }

    private static void throwNewError(String message) throws IOException {
        LOGGER.error(message);
        throw new IOException(message);
    }
}
