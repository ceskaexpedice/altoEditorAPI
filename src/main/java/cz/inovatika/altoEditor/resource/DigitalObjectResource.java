package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
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
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.OcrUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import static cz.inovatika.altoEditor.editor.AltoDatastreamEditor.nextVersion;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_FORBIDDEN;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_UNAUTHORIZED;
import static cz.inovatika.altoEditor.user.UserUtils.getToken;
import static cz.inovatika.altoEditor.user.UserUtils.getUserProfile;
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

    private static final Logger LOGGER = LogManager.getLogger(DigitalObjectResource.class.getName());

    public static void getObjectInformation(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            String pid = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            String instanceId = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);

            K7ObjectInfo objectInfo = new K7ObjectInfo();
            ObjectInformation objectInformation = objectInfo.getObjectInformation(pid, instanceId, userProfile);

            setResult(context, new AltoEditorResponse(objectInformation));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getImage(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            String pid = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(null, pid);

            String instanceId = null;
            for (DigitalObjectView digitalObject : digitalObjects) {
                if (digitalObject.getInstance() != null && !digitalObject.getInstance().isEmpty()) {
                    instanceId = digitalObject.getInstance();
                    break;
                }
            }

            if (instanceId == null) {
                instanceId = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);
            }

            K7ImageViewer imageViewer = new K7ImageViewer();
            HttpResponse response = imageViewer.getResponse(pid, instanceId, userProfile);

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
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            AltoEditorStringRecordResponse response = getAltoResponse(context, userProfile);
            setStringResult(context, response);
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAltoOriginal(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
//            String login = getOptStringRequestValue(context, Const.PARAM_USER_LOGIN);
            String pid = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);

            // posloupnost hledani pro dany pid
            // 1. defaultni verze od PERA
            // 2. defaultni verze z Krameria

            // 1. defaultni verze od PERA
            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_PERO, pid);
                if (!digitalObjects.isEmpty()) {
                    LOGGER.debug("Version find in repositories using login as PERO and pid.");
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
                    setStringResult(context, response);
                    return;
                }
            }

            // 2. defaultni verze z Krameria
            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_ALTOEDITOR, pid);
                if (!digitalObjects.isEmpty()) {
                    LOGGER.debug("Version find in repositories using login as AltoEditor and pid.");
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
                    setStringResult(context, response);
                    return;
                }
            }

            setResult(context, AltoEditorResponse.asError("Object not found in Storage", new DigitalObjectNotFoundException(pid, "Object not found in Storage")));
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }

    }

    public static void getOcr(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            AltoEditorStringRecordResponse response = getAltoResponse(context, userProfile);
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

    public static AltoEditorStringRecordResponse getAltoResponse(Context context, UserProfile userProfile) throws AltoEditorException, SQLException, IOException {
//        String login = getOptStringRequestValue(context, Const.PARAM_USER_LOGIN);
        String pid = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
        String versionXml = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_VERSION_XML);

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
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(userProfile.getUsername(), pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 3. defaultni verze od Pera
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_PERO, pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as PERO and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 4. defaultni verze z Krameria
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_ALTOEDITOR, pid);
            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as AltoEditor and pid.");
                return getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersionXml());
            }
        }

        // 5. stazeni nove verze z krameria
        String instanceId = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);

        K7Downloader downloader = new K7Downloader();
        downloader.downloadFoxml(pid, instanceId, userProfile);
        UserProfile tmpUser = new UserProfile(Const.USER_ALTOEDITOR, userProfile.getToken());
        Manager.createDigitalObject(tmpUser, pid, AltoDatastreamEditor.ALTO_ID + ".0", instanceId);
        return getAltoStream(pid, AltoDatastreamEditor.ALTO_ID + ".0");
    }

    public static void updateAlto(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String data = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_DATA);

            // hledani objektu konkretniho uzivatele
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(userProfile.getUsername(), pid);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object for user");
                } else {
                    DigitalObjectView digitalObject = digitalObjects.get(0);
                    if (digitalObject.getLock()) {
                        throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObject.getPid()));
                    }
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + userProfile.getUsername(), digitalObject.getVersionXml());
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
                    if (digitalObject.getLock()) {
                        throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObject.getPid()));
                    }
                    String versionId = nextVersion(digitalObject.getVersionXml());
                    LOGGER.debug("Object found in repositories using pid - using default version");

                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + userProfile.getUsername(), versionId);
                    Manager.createDigitalObject(userProfile, pid, versionId, digitalObject.getInstance(), Const.DIGITAL_OBJECT_STATE_EDITED);

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
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String priority = getOptStringNodeValue(node, Const.PARAM_BATCH_PRIORITY);

            // hledani objektu konkretniho uzivatele
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(Const.USER_PERO, pid);

            if (priority == null) {
                priority = Const.BATCH_PRIORITY_MEDIUM;
            }

            if (!digitalObjects.isEmpty()) {
                if (digitalObjects.get(0).getLock()) {
                    throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObjects.get(0).getPid()));
                }
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
                DigitalObjectView digitalObject = getDigitalObject(pid);
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObject.getPid()));
                }
                String instanceId = "";
                if (digitalObject != null) {
                    instanceId = digitalObject.getInstance();
                }
                if (isBlank(instanceId)) {
                    instanceId = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_INSTANCE);
                }
                Batch batch = Manager.addNewBatch(pid, priority, instanceId, 0);
                FileGeneratorProcess process = FileGeneratorProcess.prepare(batch, userProfile);
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
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            // hledani objektu konkretniho uzivatele
            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(digitalObject.getPid(), String.format("Digital object %s is locked", digitalObject.getPid()));
                }
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
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            // hledani objektu konkretniho uzivatele
            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(digitalObject.getPid(), String.format("Digital object %s is locked", digitalObject.getPid()));
                }
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
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            DigitalObjectView digitalObject = Manager.getDigitalObjectById(objectId);

            if (digitalObject != null && digitalObject.getPid() != null) {

                K7Uploader uploader = new K7Uploader();
                uploader.uploadAltoOcr(digitalObject.getPid(), digitalObject.getVersionXml(), digitalObject.getInstance(), userProfile);

                Manager.updateDigitalObjectWithStateUploaded(digitalObject);
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

    public static void lockDigitalObject(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);

            Manager.lockDigitalObject(pid);

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjectsByPid(pid, Const.PARAM_DIGITAL_OBJECT_ID, "asc");
            setResult(context, new AltoEditorResponse(digitalObjects));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void unlockDigitalObject(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);

            Manager.unlockDigitalObject(pid);

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjectsByPid(pid, Const.PARAM_DIGITAL_OBJECT_ID, "asc");
            setResult(context, new AltoEditorResponse(digitalObjects));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }
}
