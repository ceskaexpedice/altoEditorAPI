package cz.inovatika.altoEditor.kramerius;

import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage.LocalObject;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.FileUtils.createFolder;
import static cz.inovatika.altoEditor.utils.FileUtils.deleteFolder;
import static cz.inovatika.altoEditor.utils.FileUtils.getFile;
import static cz.inovatika.altoEditor.utils.FileUtils.getPidAsFile;
import static cz.inovatika.altoEditor.utils.FileUtils.writeToFile;
import static cz.inovatika.altoEditor.utils.FoxmlUtils.closeQuietly;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7Downloader {

    private static final Logger LOGGER = LogManager.getLogger(K7Downloader.class.getName());

    public void downloadFoxml(String pid, String instanceId, UserProfile userProfile) throws AltoEditorException, IOException {
        AkubraStorage storage = AkubraStorage.getInstance();
        if (storage.exist(pid)) {
            LOGGER.warn("Object already exists in repo");
            return;
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        String foxml = download(instance, pid, userProfile.getToken());
        saveToTmp(foxml, pid);

        if (!containsAlto(pid)) {
            String alto = downloadAlto(instance, pid, userProfile.getToken());
            saveAltoToTmp(alto, pid);
            updateFoxml(pid);
        }

        importToStorage(pid, userProfile.getUsername());
        deleteFromTmp(pid);
    }

    private void deleteFromTmp(String pid) throws AltoEditorException {
        File pidFile = getFile(pid);
        deleteFolder(pidFile);
        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        if (altoFile.exists()) {
            deleteFolder(altoFile);
        }
    }

    private void updateFoxml(String pid) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);
        if (pidFile == null || !pidFile.exists() || !pidFile.canRead() || !pidFile.canWrite()) {
            throw new IOException("Can not read foxml: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalObject lObj = localStorage.load(pid, pidFile);

        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        if (pidFile == null || !pidFile.exists() || !pidFile.canRead() || !pidFile.canWrite()) {
            throw new IOException("Can not read foxml: " + pidFile.getAbsolutePath());
        }
        AltoDatastreamEditor.importAlto(lObj, altoFile.toURI(), "Default ALTO", AltoDatastreamEditor.ALTO_ID + ".0");
        lObj.flush();
    }

    private boolean containsAlto(String pid) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);
        if (pidFile == null || !pidFile.exists() || !pidFile.canRead() || !pidFile.canWrite()) {
            throw new IOException("Can not read foxml: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalObject lObj = localStorage.load(pid, pidFile);

        if (lObj != null && lObj.getDigitalObject() != null && lObj.getDigitalObject().getDatastream() != null) {
            for (DatastreamType datastream : lObj.getDigitalObject().getDatastream()) {
                if (AltoDatastreamEditor.ALTO_ID.equals(datastream.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void importToStorage(String pid, String login) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);

        if (pidFile == null || !pidFile.exists() || !pidFile.canRead()) {
            throw new IOException("Can not read foxml: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalObject lObj = localStorage.load(pid, pidFile);

        AkubraStorage akubraStorage = AkubraStorage.getInstance();
        akubraStorage.ingest(lObj, login, "Ingested by AltoEditor from Kramerius");
    }

    private String download(KrameriusOptions.KrameriusInstance instance, String pid, String token) throws IOException {
        String foxmlUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId()) +
                pid + "/foxml";
        LOGGER.info("Trying to download FOXML from " + foxmlUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(foxmlUrl);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        HttpResponse response = httpClient.execute(httpGet);
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Download FOXML success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return result;
                } else {
                    LOGGER.warn("Downloaded FOXML but result is null or empty");
                    throw new IOException("Downloaded FOXML but result is null or empty");
                }
            } else {
                LOGGER.warn("Downloaded FOXML but entity is null");
                throw new IOException("Downloaded FOXML but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private String downloadAlto(KrameriusOptions.KrameriusInstance instance, String pid, String token) throws IOException {
        String foxmlUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId()) +
                pid + "/ocr/alto";
        LOGGER.info("Trying to download Alto from " + foxmlUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(foxmlUrl);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        HttpResponse response = httpClient.execute(httpGet);
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Download FOXML success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return result;
                } else {
                    LOGGER.warn("Downloaded FOXML but result is null or empty");
                    throw new IOException("Downloaded FOXML but result is null or empty");
                }
            } else {
                LOGGER.warn("Downloaded FOXML but entity is null");
                throw new IOException("Downloaded FOXML but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private void saveToTmp(String foxmlContent, String pid) throws AltoEditorException, IOException {
        File foxmlFile = getFile(pid);
        writeToFile(foxmlContent, foxmlFile, pid);
    }

    private void saveAltoToTmp(String altoContent, String pid) throws AltoEditorException, IOException {
        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        writeToFile(altoContent, altoFile, pid);
    }

    public File saveImage(String pid, String instanceId, UserProfile userProfile) throws AltoEditorException, IOException {
        return saveImage(pid, pid, instanceId, userProfile);
    }

    public File saveImage(String parentPid, String pid, String instanceId, UserProfile userProfile) throws AltoEditorException, IOException {
        File peroPath = createFolder(new File(Config.getPeroPath()), false);
        File parentFile = createFolder(new File(peroPath, getPidAsFile(parentPid)), true);



        K7ObjectInfo k7ObjectInfo = new K7ObjectInfo();
        String model = k7ObjectInfo.getModel(pid, instanceId, userProfile);
        if (model == null) {
            throw new IOException("Unknown model for pid = " + pid);
        }
        if ("page".equals(model)) {
            File imageFile = getFile(parentFile, pid, "IMAGE");
            InputStream imageContent = null;
            try {
                imageContent = downloadImage(pid, instanceId, userProfile);
                writeToFile(imageContent, imageFile, pid);
            } finally {
                closeQuietly(imageContent, pid);
            }
        } else {
            List<String> childrenPids = k7ObjectInfo.getChildrenPids(pid, instanceId, userProfile);
            for (String childrenPid : childrenPids) {
                File imageFile = getFile(parentFile, childrenPid, "IMAGE");
                InputStream imageContent = null;
                try {
                    imageContent = downloadImage(childrenPid, instanceId, userProfile);
                    writeToFile(imageContent, imageFile, childrenPid);
                } finally {
                    closeQuietly(imageContent, childrenPid);
                }
            }
        }
        return parentFile;
    }

    private InputStream downloadImage(String pid, String instanceId, UserProfile userProfile) throws IOException, AltoEditorException {
        K7ImageViewer imageViewer = new K7ImageViewer();
        HttpResponse response = imageViewer.getResponse(pid, instanceId, userProfile);

        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Download FOXML success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return entity.getContent();
//                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
//                if (result != null && !result.isEmpty()) {
//                    return result;
//                } else {
//                    LOGGER.warn("Downloaded FOXML but result is null or empty");
//                    throw new IOException("Downloaded FOXML but result is null or empty");
//                }
            } else {
                LOGGER.warn("Downloaded Image but entity is null");
                throw new IOException("Downloaded Image but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("Downloading Image ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("Downloaded Image ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("Downloading Image ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Downloading Image ended with code " + response.getStatusLine().getStatusCode());
        }

    }
}
