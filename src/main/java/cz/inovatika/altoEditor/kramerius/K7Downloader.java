package cz.inovatika.altoEditor.kramerius;

import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamVersionType;
import com.yourmediashelf.fedora.generated.foxml.StateType;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage.LocalObject;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.FoxmlUtils;
import cz.inovatika.altoEditor.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.FoxmlUtils.closeQuietly;
import static cz.inovatika.altoEditor.utils.FoxmlUtils.deleteFolder;
import static cz.inovatika.altoEditor.utils.FoxmlUtils.getPidAsFile;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7Downloader {

    private static final Logger LOG = LoggerFactory.getLogger(K7Downloader.class.getName());

    public void downloadFoxml(String pid, String instanceId, String login) throws AltoEditorException, IOException {
        AkubraStorage storage = AkubraStorage.getInstance();
        if (storage.exist(pid)) {
            LOG.warn("Object already exists in repo");
            return;
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        K7Authenticator authenticator = new K7Authenticator(instance);
        String token = authenticator.authenticate();

        if (token == null || token.isEmpty()) {
            LOG.error("Kramerius token is null");
            throw new AltoEditorException(instanceId, "Kramerius token is null");
        }

        String foxml = download(instance, pid, token);
        saveToTmp(foxml, pid);

        if (!containsAlto(pid)) {
            String alto = downloadAlto(instance, pid, token);
            saveAltoToTmp(alto, pid);
            updateFoxml(pid);
        }

        importToStorage(pid, login);
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
        LOG.info("Trying to download FOXML from " + foxmlUrl);

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
            LOG.debug("Http response Download FOXML success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return result;
                } else {
                    LOG.warn("Downloaded FOXML but result is null or empty");
                    throw new IOException("Downloaded FOXML but result is null or empty");
                }
            } else {
                LOG.warn("Downloaded FOXML but entity is null");
                throw new IOException("Downloaded FOXML but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOG.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOG.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private String downloadAlto(KrameriusOptions.KrameriusInstance instance, String pid, String token) throws IOException {
        String foxmlUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId()) +
                pid + "/ocr/alto";
        LOG.info("Trying to download Alto from " + foxmlUrl);

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
            LOG.debug("Http response Download FOXML success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return result;
                } else {
                    LOG.warn("Downloaded FOXML but result is null or empty");
                    throw new IOException("Downloaded FOXML but result is null or empty");
                }
            } else {
                LOG.warn("Downloaded FOXML but entity is null");
                throw new IOException("Downloaded FOXML but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOG.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOG.warn("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("Downloaded FOXML ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOG.warn("Downloading FOXML ended with code " + response.getStatusLine().getStatusCode());
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

    private void writeToFile(String content, File file, String pid) throws IOException {
        if (file.exists()) {
            deleteFolder(file);
        }

        if (!file.createNewFile()) {
            LOG.warn("Can not create file " + file.getAbsolutePath());
            throw new IOException("Nepodařilo se vytvořit soubor " + file.getAbsolutePath());
        }

        FileOutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(content);
        } finally {
            closeQuietly(outputStreamWriter, pid);
            closeQuietly(outputStream, pid);
        }
    }

    private File getFile(String pid) throws AltoEditorException {
        return getFile(pid, null);
    }

    private File getFile(String pid, String stream) throws AltoEditorException {
        File tmpFolder = new File(Utils.getDefaultHome(), "tmp");
        if (!tmpFolder.exists()) {
            if (!tmpFolder.mkdirs()) {
                LOG.error("It is not possible to create tmp folder in " + Utils.getDefaultHome().getAbsolutePath());
                throw new AltoEditorException("It is not possible to create tmp folder in " + Utils.getDefaultHome().getAbsolutePath());
            }
        }
        File pidFile;
        if (stream != null && !stream.isEmpty()) {
            pidFile = new File(tmpFolder, stream + "_" + getPidAsFile(pid) + ".xml");
        } else {
            pidFile = new File(tmpFolder, getPidAsFile(pid) + ".xml");
        }
        return pidFile;
    }
}
