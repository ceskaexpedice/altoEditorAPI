package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import cz.inovatika.altoEditor.kramerius.K7ImageViewer;
import cz.inovatika.altoEditor.kramerius.K7ObjectInfo;
import cz.inovatika.altoEditor.kramerius.K7Uploader;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.models.ObjectInformation;
import cz.inovatika.altoEditor.process.FileGeneratorProcess;
import cz.inovatika.altoEditor.process.ProcessDispatcher;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.response.AltoEditorStringRecordResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage.AkubraObject;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.OcrUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.elasticsearch.common.recycler.Recycler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.editor.AltoDatastreamEditor.nextVersion;
import static cz.inovatika.altoEditor.utils.Utils.getIntegerNodeValue;
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

    public static void getObjectInformation(@NotNull Context context) {
        try {
            String login = getOptStringRequestValue(context, "login");
            String pid = getOptStringRequestValue(context, "pid");
            String instanceId = getOptStringRequestValue(context, "instance");

            K7ObjectInfo objectInfo = new K7ObjectInfo();
            ObjectInformation objectInformation = objectInfo.getObjectInformation(pid, instanceId);

            setResult(context, new AltoEditorResponse(objectInformation));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getImage(Context context) {
        try {
            String login = getOptStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(null, pid);

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

    public static void getAlto(@NotNull Context context) {
        try {
            AltoEditorStringRecordResponse response = getAltoResponse(context);
            setStringResult(context, response);
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getOcr(@NotNull Context context) {
        try {
            AltoEditorStringRecordResponse response = getAltoResponse(context);
            if (response != null && response.getData() != null) {
                String altoStream = (String) response.getData();
                String ocr = OcrUtils.createOcrFromAlto(altoStream);
                response.setData(ocr);
            } else {
                if (response == null) {
                    response = new AltoEditorStringRecordResponse();
                }
                response.setData(null);
            }
            setStringResult(context, response);
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }

    }

    public static AltoEditorStringRecordResponse getAltoResponse(Context context) throws AltoEditorException, SQLException, IOException {
        String login = getOptStringRequestValue(context, "login");
        String pid = getStringRequestValue(context, "pid");
        String versionXml = getOptStringRequestValue(context, "versionXml");

        // posloupnost hledani pro dany pid
        // 1. podle verze
        // 2. podle uzivatele
        // 3. defaulni verze od PERA
        // 4. defaulni verze z Krameria
        // 5. stazeni nove verze z Krameria

        // 1. podle verze
        if (versionXml != null && !versionXml.isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(null, pid);
            for (DigitalObjectView digitalObject : digitalObjects) {
                LOGGER.debug("Version find in repositories using version and pid.");
                if (versionXml.equals(digitalObject.getVersionXml())) {
                    return getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                }
            }
        }

        // 2. podle uzivatele
        if (login != null && !login.isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(login, pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 3. defaultni verze od Pera
        if (login != null && !login.isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_PERO, pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as PERO and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 4. defaultni verze z Krameria
        if (login != null && !login.isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_ALTOEDITOR, pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as AltoEditor and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 5. stazeni nove verze z krameria
        String instanceId = getStringRequestValue(context, "instance");

        K7Downloader downloader = new K7Downloader();
        downloader.downloadFoxml(pid, instanceId, login);
        Manager.createDigitalObject(Const.USER_ALTOEDITOR, pid, AltoDatastreamEditor.ALTO_ID + ".0", instanceId);
        return getAltoStream(pid, AltoDatastreamEditor.ALTO_ID + ".0");
    }

    public static void updateAlto(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            String pid = getStringNodeValue(node, "pid");
            String data = getStringNodeValue(node, "data");


            // hledani objektu konkretniho uzivatele
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(login, pid);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object for user");
                } else {
                    DigitalObjectView digitalObject = digitalObjects.get(0);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + login, digitalObject.getVersionXml());
                    Manager.updateDigitalObject(digitalObject.getId(), digitalObject.getVersionXml());

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersionXml());
                    setStringResult(context, response);
                    return;
                }
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = Manager.getDigitalObjectsWithMaxVersionByPid(pid);
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
                    Manager.createDigitalObject(login, pid, versionId, digitalObject.getInstance(), Const.DIGITAL_OBJECT_STATE_EDITED);

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), versionId);
                    setStringResult(context, response);
                    return;
                }
            } else {
                throw new DigitalObjectNotFoundException(pid, String.format("This pid \"%s\" not found in repository.", pid));
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

            // hledani objektu konkretniho uzivatele
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_PERO, pid);

            if (priority == null) {
                priority = Const.BATCH_PRIORITY_MEDIUM;
            }

            if (!digitalObjects.isEmpty()) {
                String versionXml = AltoDatastreamEditor.ALTO_ID + ".1";
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
                DigitalObjectView dObj = getDigitalObject(pid);
                String instanceId = "";
                if (dObj != null) {
                    instanceId = dObj.getInstance();
                }
                if (isBlank(instanceId)) {
                    instanceId = getStringNodeValue(node, "instance");
                }
                Batch batch = Manager.addNewBatch(pid, priority, instanceId, 0);
                FileGeneratorProcess process = FileGeneratorProcess.prepare(batch);
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

    public static void stateAccepted(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            Integer objectId = getIntegerNodeValue(node, "id");

            // hledani objektu konkretniho uzivatele
            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                Manager.updateDigitalObjectWithState(objectId, Const.DIGITAL_OBJECT_STATE_ACCEPTED);
                digitalObject = Manager.getDigitalObjectById(objectId);
                setResult(context, new AltoEditorResponse(digitalObject));
            } else {
                throw new DigitalObjectNotFoundException(String.valueOf(objectId), String.format("This objectId \"%s\" not found in repository.", objectId));
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void stateRejected(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            Integer objectId = getIntegerNodeValue(node, "id");

            // hledani objektu konkretniho uzivatele
            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                Manager.updateDigitalObjectWithState(objectId, Const.DIGITAL_OBJECT_STATE_REJECTED);
                digitalObject = Manager.getDigitalObjectById(objectId);
                setResult(context, new AltoEditorResponse(digitalObject));
            } else {
                throw new DigitalObjectNotFoundException(String.valueOf(objectId), String.format("This objectId \"%s\" not found in repository.", objectId));
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    private static DigitalObjectView getDigitalObject(String pid) throws SQLException {
        List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(null, pid);
        if (!digitalObjects.isEmpty()) {
            for (DigitalObjectView digitalObject : digitalObjects) {
                if (!isBlank(digitalObject.getInstance())) {
                    return digitalObject;
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
        response.setVersion(versionId);
        return response;
    }

    public static void uploadKramerius(@NotNull Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            Integer objectId = getIntegerNodeValue(node, "id");

            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null && digitalObject.getPid() != null) {

                K7Uploader uploader = new K7Uploader();
                uploader.uploadAltoOcr(digitalObject.getPid(), digitalObject.getVersionXml(), digitalObject.getInstance());

                Manager.updateDigitalObjectWithState(objectId, Const.DIGITAL_OBJECT_STATE_UPLOADED);
                digitalObject = Manager.getDigitalObjectById(objectId);

                // response po uspesnem zapsani do Krameria
                AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
                response.setPid(digitalObject.getPid());
                response.setContent("Nahráno do Krameria");
                setStringResult(context, response);
            } else {
                LOGGER.warn("Digital Object not find in respositories using objectId: " + objectId);
                throw new DigitalObjectNotFoundException(String.valueOf(objectId), "Digital Object not find in respositories using objectId: " + objectId);
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }
}
