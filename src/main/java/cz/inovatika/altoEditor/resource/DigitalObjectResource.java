package cz.inovatika.altoEditor.resource;

import cz.inovatika.altoEditor.db.filter.DigitalObjectFilter;
import cz.inovatika.altoEditor.db.manager.BatchManager;
import cz.inovatika.altoEditor.db.manager.DigitalObjectManager;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.kramerius.K7Client;
import cz.inovatika.altoEditor.kramerius.K7Utility;
import cz.inovatika.altoEditor.kramerius.KrameriusOptions;
import cz.inovatika.altoEditor.process.PeroOcrProcessor;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.model.Batch;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import static cz.inovatika.altoEditor.editor.AltoDatastreamEditor.nextVersion;
import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_FORBIDDEN;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_UNAUTHORIZED;
import static cz.inovatika.altoEditor.user.UserUtils.getToken;
import static cz.inovatika.altoEditor.user.UserUtils.getUserProfile;
import static cz.inovatika.altoEditor.utils.Const.DIGITAL_OBJECT_STATE_UPLOADED;
import static cz.inovatika.altoEditor.utils.Utils.getIntegerNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptIntegerNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptIntegerRequestValue;
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

            try (K7Client client = new K7Client(findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId))) {
                ObjectInformation objectInformation = client.getObjectInformation(pid, userProfile.getToken());
                setResult(context, new AltoEditorResponse(objectInformation));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getSiblings(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            Integer id = getOptIntegerRequestValue(context, Const.PARAM_DIGITAL_OBJECT_ID);
            String pid = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            String instanceId = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);

            if (!((id != null && id > 0) || ((pid != null && !pid.isBlank()) && (instanceId != null && !instanceId.isBlank())))) {
                throw new RequestException("parameters", "Missing parameters {}, {}, {}".formatted(Const.PARAM_DIGITAL_OBJECT_ID, Const.PARAM_DIGITAL_OBJECT_PID, Const.PARAM_DIGITAL_OBJECT_INSTANCE));
            }

            DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
            if (id != null) {
                DigitalObject digitalObject = digitalObjectManager.getDigitalObject(id);

                if (digitalObject != null) {
                    pid = digitalObject.getPid();
                    instanceId = digitalObject.getInstance();
                }
            }


            try (K7Client client = new K7Client(findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId))) {
                ObjectInformation objectInformation = client.getObjectInformation(pid, userProfile.getToken());
                String parentPid = objectInformation.getParentPid();
                String json;
                if (parentPid == null) {
                    json = getPidNavigationAsString(Collections.emptyList(), pid, instanceId);
                } else {
                    List<String> pids = client.getChildrenPids(objectInformation.getParentPid(), userProfile.getToken());
                    json = getPidNavigationAsString(pids, pid, instanceId);
                }
                setResult(context, json);
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static String getPidNavigationAsString(List<String> pidList, String currentPid, String instanceId) {
        JSONObject result = new JSONObject();

        int index = pidList.indexOf(currentPid);
        if (index == -1) {
            result.put("pid", currentPid);
            result.put("instance", instanceId);
            result.put("previousPid", JSONObject.NULL);
            result.put("nextPid", JSONObject.NULL);
            return result.toString();
        }

        String previous = index > 0 ? pidList.get(index - 1) : null;
        String next = index < pidList.size() - 1 ? pidList.get(index + 1) : null;

        result.put("pid", currentPid);
        result.put("instance", instanceId);
        result.put("previousPid", previous != null ? previous : JSONObject.NULL);
        result.put("nextPid", next != null ? next : JSONObject.NULL);

        return result.toString();
    }

    public static void getImage(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            String pid = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

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

            K7Client client = new K7Client(findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId));
            HttpResponse response = client.getResponse(pid, userProfile.getToken());

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
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
//            String login = getOptStringRequestValue(context, Const.PARAM_USER_LOGIN);
            String pid = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);

            // posloupnost hledani pro dany pid
            // 1. defaultni verze od PERA
            // 2. defaultni verze z Krameria

            // 1. defaultni verze od PERA
            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                DigitalObjectFilter filter = DigitalObjectFilter.builder().login(Const.USER_PERO).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
                List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

                if (!digitalObjects.isEmpty()) {
                    LOGGER.debug("Version find in repositories using login as PERO and pid.");
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0));
                    setStringResult(context, response);
                    return;
                }
            }

            // 2. defaultni verze z Krameria
            if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {
                DigitalObjectFilter filter = DigitalObjectFilter.builder().login(Const.USER_ALTOEDITOR).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
                List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

                if (!digitalObjects.isEmpty()) {
                    LOGGER.debug("Version find in repositories using login as AltoEditor and pid.");
                    AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0));
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
        Objects.requireNonNull(userProfile, "User profile is null.");

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

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();

        // posloupnost hledani pro dany pid
        // 1. podle verze
        // 2. podle uzivatele
        // 3. defaulni verze od PERA
        // 4. defaulni verze z Krameria
        // 5. stazeni nove verze z Krameria

        // 1. podle verze
        if (versionXml != null && !versionXml.isEmpty()) {

            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            for (DigitalObjectView digitalObject : digitalObjects) {
                LOGGER.debug("Version find in repositories using version and pid.");
                if (versionXml.equals(digitalObject.getVersion())) {
                    return getAltoStream(digitalObject);
                }
            }
        }

        // 2. podle uzivatele
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {

            DigitalObjectFilter filter = DigitalObjectFilter.builder().login(userProfile.getUsername()).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid.");
                return getAltoStream(digitalObjects.get(0));
            }
        }

        // 3. defaultni verze od Pera
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {

            DigitalObjectFilter filter = DigitalObjectFilter.builder().login(Const.USER_PERO).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as PERO and pid.");
                return getAltoStream(digitalObjects.get(0));
            }
        }

        // 4. defaultni verze z Krameria
        if (userProfile.getUsername() != null && !userProfile.getUsername().isEmpty()) {

            DigitalObjectFilter filter = DigitalObjectFilter.builder().login(Const.USER_ALTOEDITOR).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login as AltoEditor and pid.");
                return getAltoStream(digitalObjects.get(0));
            }
        }

        // 5. stazeni nove verze z krameria
        String instanceId = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);

        ObjectInformation info = null;
        try (K7Client client = new K7Client(findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId))) {
            info = client.getObjectInformation(pid, userProfile.getToken());
            K7Utility downloader = new K7Utility();
            downloader.downloadFoxml(pid, info == null ? Const.DIGITAL_OBJECT_MODEL_PAGE : info.getModel(), client, userProfile);
        }
        UserProfile tmpUser = new UserProfile(Const.USER_ALTOEDITOR, userProfile.getToken());

        DigitalObject digitalObject = null;
        if (info != null) {
            digitalObject = digitalObjectManager.addNewDigitalObject(tmpUser.getUsername(), pid, "0", instanceId, Const.DIGITAL_OBJECT_STATE_NEW, info.getModel(), info.getLabel(), info.getParentLabel(), info.getParentPath());
        } else {
            digitalObject = digitalObjectManager.addNewDigitalObject(tmpUser.getUsername(), pid, "0", instanceId);
        }

        return getAltoStream(digitalObject);
    }

    public static void updateAlto(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String data = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_DATA);

            // hledani objektu konkretniho uzivatele
            DigitalObjectFilter filter = DigitalObjectFilter.builder().login(userProfile.getUsername()).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            if (!digitalObjects.isEmpty()) {
                LOGGER.debug("Version find in repositories using login and pid");
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object for user");
                } else {
                    DigitalObject digitalObject = digitalObjectManager.getDigitalObject(digitalObjects.get(0).getId());

                    if (digitalObject.getLock()) {
                        throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObject.getPid()));
                    }
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + userProfile.getUsername(), digitalObject.getVersion());

                    digitalObject.setVersion(digitalObject.getVersion());

                    digitalObject = digitalObjectManager.updateDigitalObject(digitalObject);

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObject);
                    setStringResult(context, response);
                    return;
                }
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = digitalObjectManager.getDigitalObjectsWithMaxVersionByPid(pid);
            if (!digitalObjects.isEmpty()) {
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object with max version");
                } else {
                    DigitalObject digitalObject = digitalObjectManager.getDigitalObject(digitalObjects.get(0).getId());

                    if (digitalObject.getLock()) {
                        throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObject.getPid()));
                    }
                    String versionId = nextVersion(digitalObject.getVersion());
                    LOGGER.debug("Object found in repositories using pid - using default version");

                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + userProfile.getUsername(), versionId);

                    digitalObject = digitalObjectManager.addNewDigitalObject(userProfile.getUsername(), digitalObject.getPid(), versionId, digitalObject.getInstance(), Const.DIGITAL_OBJECT_STATE_EDITED, digitalObject);

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObject);
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
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String priority = getOptStringNodeValue(node, Const.PARAM_BATCH_PRIORITY);
            Integer ocrEngine = getOptIntegerNodeValue(node, Const.PARAM_BATCH_OCR_ENGINE);

            // hledani objektu konkretniho uzivatele
            DigitalObjectFilter filter = DigitalObjectFilter.builder().login(Const.USER_PERO).pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            if (priority == null) {
                priority = Const.BATCH_PRIORITY_MEDIUM;
            }

            if (digitalObjects.isEmpty()) {
                DigitalObjectView digitalObject = getDigitalObject(pid);
                if (digitalObject != null) {
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
                    BatchManager batchManager = BatchManager.getInstance();
                    Batch batch = batchManager.addNewBatch(pid, priority, instanceId, ocrEngine, digitalObject.getId());

                    FileGeneratorProcess process = FileGeneratorProcess.prepare(batch, userProfile);
                    ProcessDispatcher.getDefault().addPeroProcess(process);
                    AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
                    response.setPid(pid);
                    response.setContent("Proces zapsán");
                    setStringResult(context, response);
                }
            } else if (digitalObjects.size() == 1) {
                DigitalObjectView digitalObject = digitalObjects.get(0);
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(pid, String.format("Digital object %s is locked", digitalObjects.get(0).getPid()));
                }
                AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0));
                setStringResult(context, response);
            } else {
                for (DigitalObjectView digitalObject : digitalObjects) {
                    if ((AltoDatastreamEditor.ALTO_ID + ".1").equals(digitalObject.getVersion())) {
                        AltoEditorStringRecordResponse response = getAltoStream(digitalObject);
                        setStringResult(context, response);
                        return;
                    }
                }
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getPeroEngines(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }

        List<PeroOcrProcessor.PeroOcrEnginesDescriptor> descriptors;
        try {
            PeroOcrProcessor ocrProcessor = new PeroOcrProcessor();
            descriptors = ocrProcessor.getEnginesList();
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Chyba při načítání PERO engines", e);
            descriptors = Collections.emptyList();
        }

        setResult(context, new AltoEditorResponse(descriptors, 0, descriptors.size()));
    }

    public static void stateAccepted(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            // hledani objektu konkretniho uzivatele

            DigitalObject digitalObject = digitalObjectManager.getDigitalObject(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(digitalObject.getPid(), String.format("Digital object %s is locked", digitalObject.getPid()));
                }

                digitalObject.setState(Const.DIGITAL_OBJECT_STATE_ACCEPTED);
                digitalObject = digitalObjectManager.updateDigitalObject(digitalObject);

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
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            // hledani objektu konkretniho uzivatele
            DigitalObject digitalObject = digitalObjectManager.getDigitalObject(objectId);

            if (digitalObject != null) {
                LOGGER.debug("Digital Object find in repositories using objectId");
                if (digitalObject.getLock()) {
                    throw new DigitalObjectException(digitalObject.getPid(), String.format("Digital object %s is locked", digitalObject.getPid()));
                }

                digitalObject.setState(Const.DIGITAL_OBJECT_STATE_REJECTED);
                digitalObject = digitalObjectManager.updateDigitalObject(digitalObject);

                setResult(context, new AltoEditorResponse(digitalObject));
            } else {
                throw new DigitalObjectNotFoundException(String.valueOf(objectId), String.format("This objectId \"%s\" not found in repository.", objectId));
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    private static DigitalObjectView getDigitalObject(String pid) {
        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();

        DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
        List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);
        if (!digitalObjects.isEmpty()) {
            for (DigitalObjectView digitalObject : digitalObjects) {
                if (!isBlank(digitalObject.getInstance())) {
                    return digitalObject;
                }
            }
        }
        return null;
    }

    private static AltoEditorStringRecordResponse getAltoStream(DigitalObjectView digitalObject) throws IOException, AltoEditorException {
        if (Const.DIGITAL_OBJECT_MODEL_PAGE.equals(digitalObject.getModel())) {
            return getAltoStream(digitalObject.getPid(), digitalObject.getVersion(), digitalObject.getModel(), digitalObject.getState());
        } else {
            AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
            response.setPid(digitalObject.getPid());
            response.setContent("This digital object does not contain a alto xml.");
            response.setModel(digitalObject.getModel());
            response.setVersionState(digitalObject.getState());
            return response;
        }
    }

    private static AltoEditorStringRecordResponse getAltoStream(DigitalObject digitalObject) throws IOException, AltoEditorException {
        if (Const.DIGITAL_OBJECT_MODEL_PAGE.equals(digitalObject.getModel())) {
            return getAltoStream(digitalObject.getPid(), digitalObject.getVersion(), digitalObject.getModel(), digitalObject.getState());
        } else {
            AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
            response.setPid(digitalObject.getPid());
            response.setContent("This digital object does not contain a alto xml.");
            response.setModel(digitalObject.getModel());
            response.setVersionState(digitalObject.getState());
            return response;
        }
    }

    private static AltoEditorStringRecordResponse getAltoStream(@NotNull String pid, @NotNull String versionId, @NotNull String model, @NotNull String state) throws IOException, AltoEditorException {
        AkubraStorage storage = AkubraStorage.getInstance();
        AkubraObject object = storage.find(pid);
        AltoDatastreamEditor altoEditor = AltoDatastreamEditor.alto(object);
        AltoEditorStringRecordResponse response = altoEditor.readRecord(versionId);
        response.setPid(pid);
        response.setVersion(versionId);
        response.setModel(model);
        response.setVersionState(state);
        return response;
    }

    public static void uploadKramerius(@NotNull Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            Integer objectId = getIntegerNodeValue(node, Const.PARAM_DIGITAL_OBJECT_ID);

            DigitalObject digitalObject = digitalObjectManager.getDigitalObject(objectId);

            if (digitalObject != null && digitalObject.getPid() != null) {

                K7Utility uploader = new K7Utility();
                uploader.uploadAltoOcr(digitalObject, userProfile);

                digitalObject.setState(DIGITAL_OBJECT_STATE_UPLOADED);
                digitalObject.setLock(true);
                digitalObject = digitalObjectManager.updateDigitalObject(digitalObject);

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
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);

            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjectsView = digitalObjectManager.findDigitalObject(filter);
            for (DigitalObjectView digitalObjectView : digitalObjectsView) {
                if (digitalObjectsView != null && digitalObjectView.getLock()) {
                    LOGGER.error(String.format("Digital object %s is locked", digitalObjectView));
                } else {
                    DigitalObject digitalObject = digitalObjectManager.getDigitalObject(digitalObjectView.getId());
                    digitalObject.setLock(true);

                    digitalObjectManager.updateDigitalObject(digitalObject);
                }
            }

//            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid).orderBy(Const.PARAM_DIGITAL_OBJECT_ID).orderSort("asc").build();
//            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            setResult(context, "OK");
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
        Objects.requireNonNull(userProfile, "User profile is null.");

        DigitalObjectManager digitalObjectManager = DigitalObjectManager.getInstance();
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
//            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);

            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid == null ? Collections.emptyList() : List.of(pid)).build();
            List<DigitalObjectView> digitalObjectsView = digitalObjectManager.findDigitalObject(filter);
            for (DigitalObjectView digitalObjectView : digitalObjectsView) {
                if (!digitalObjectView.getLock()) {
                    LOGGER.error(String.format("Digital object %s is unlocked", digitalObjectView));
                } else {
                    DigitalObject digitalObject = digitalObjectManager.getDigitalObject(digitalObjectView.getId());
                    digitalObject.setLock(false);

                    digitalObjectManager.updateDigitalObject(digitalObject);
                }
            }

//            DigitalObjectFilter filter = DigitalObjectFilter.builder().pid(pid).orderBy(Const.PARAM_DIGITAL_OBJECT_ID).orderSort("asc").build();
//            List<DigitalObjectView> digitalObjects = digitalObjectManager.findDigitalObject(filter);

            setResult(context, "OK");
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }
}
