package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Dao;
import cz.inovatika.altoEditor.db.DigitalObject;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import cz.inovatika.altoEditor.kramerius.K7ImageViewer;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.response.AltoEditorStringRecordResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage.AkubraObject;
import cz.inovatika.altoEditor.utils.Const;
import java.io.IOException;
import java.util.List;
import org.apache.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.db.DigitalObject.getObjectWithMaxVersion;
import static cz.inovatika.altoEditor.editor.AltoDatastreamEditor.nextVersion;
import static cz.inovatika.altoEditor.utils.Utils.getStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.setResult;
import static cz.inovatika.altoEditor.utils.Utils.setStringResult;
import static java.net.HttpURLConnection.HTTP_OK;

public class DigitalObjectResource {

    private static final Logger LOG = LoggerFactory.getLogger(DigitalObjectResource.class.getName());

    public static void getImage(Context context) {
        try {
            String login = getStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");

            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(null, pid);

            String instanceId = null;
            for (DigitalObject digitalObject : digitalObjects) {
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
            String login = getStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");

            // hledani objektu konkretniho uzivatele
            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(login, pid);

            if (!digitalObjects.isEmpty()) {
                LOG.debug("Version find in repositories using login and pid");
                AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), digitalObjects.get(0).getVersion());
                setStringResult(context, response);
                return;
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = dbDao.getDigitalObjects(null, pid);

            if (!digitalObjects.isEmpty()) {
                LOG.debug("Object found in repositories using pid - return default version");
                AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), AltoDatastreamEditor.ALTO_ID + ".0");
                setStringResult(context, response);
                return;
            }

            // stazeni noveho objektu z krameria
            String instanceId = getStringRequestValue(context, "instance");

            K7Downloader downloader = new K7Downloader();
            downloader.downloadFoxml(pid, instanceId, login);
            dbDao.createDigitalObject("AltoEditor", pid, AltoDatastreamEditor.ALTO_ID + ".0", instanceId);
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
            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(login, pid);

            if (!digitalObjects.isEmpty()) {
                LOG.debug("Version find in repositories using login and pid");
                if (digitalObjects.size() > 1) {
                    throw new DigitalObjectException(pid, "More than one object for user");
                } else {
                    DigitalObject digitalObject = digitalObjects.get(0);
                    AkubraStorage storage = AkubraStorage.getInstance();
                    AkubraObject akubraObject = storage.find(pid);
                    AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + login, digitalObject.getVersion());
                    dbDao.updateDigitalObject(digitalObject.getId(), digitalObject.getVersion());

                    AltoEditorStringRecordResponse response = getAltoStream(digitalObject.getPid(), digitalObject.getVersion());
                    setStringResult(context, response);
                    return;
                }
            }

            // hledani objektu jineho uzivatele - zobrazeni defaultni verze
            digitalObjects = dbDao.getDigitalObjects(null, pid);
            if (!digitalObjects.isEmpty()) {
                DigitalObject digitalObject = getObjectWithMaxVersion(digitalObjects);
                String versionId = nextVersion(digitalObject.getVersion());
                LOG.debug("Object found in repositories using pid - using default version");

                AkubraStorage storage = AkubraStorage.getInstance();
                AkubraObject akubraObject = storage.find(pid);
                AltoDatastreamEditor.updateAlto(akubraObject, data, "ALTO updated by user " + login, versionId);
                dbDao.createDigitalObject(login, pid, versionId, digitalObject.getInstance(), Const.DIGITAL_OBJECT_STATE_EDITED);

                AltoEditorStringRecordResponse response = getAltoStream(digitalObjects.get(0).getPid(), versionId);
                setStringResult(context, response);
                return;
            } else {
                throw new DigitalObjectNotFoundException(pid, String.format("This pid \"%s\" not found in repository."));
            }
        } catch (Throwable ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
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
        LOG.error(message);
        throw new IOException(message);
    }
}
