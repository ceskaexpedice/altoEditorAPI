package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.OcrUtils.createOcrFromAlto;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7Uploader {

    private static final Logger LOGGER = LogManager.getLogger(K7Uploader.class.getName());

    public void uploadAltoOcr(DigitalObject digitalObject, UserProfile userProfile) throws AltoEditorException, IOException, InterruptedException {
        if (Const.DIGITAL_OBJECT_MODEL_OTHER.equals(digitalObject.getModel())) {
            uploadBatchAltoOcr(digitalObject, userProfile);
        } else {
            uploadAltoOcr(digitalObject.getPid(), digitalObject.getVersion(), digitalObject.getInstance(), userProfile);
        }
    }

    private void uploadBatchAltoOcr(DigitalObject digitalObject, UserProfile userProfile) throws AltoEditorException, IOException, InterruptedException {

        File parentFile = FileUtils.getPeroTmpFolder(digitalObject.getModel(), digitalObject.getPid(), false);

        if (parentFile == null || !parentFile.exists() || !parentFile.isDirectory()) {
            throw new DigitalObjectNotFoundException(digitalObject.getPid(), "Temporary folder not found for this digital object.");
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), digitalObject.getInstance());
        if (instance == null) {
            throw new DigitalObjectNotFoundException(digitalObject.getInstance(), String.format("This instance \"%s\" is not configured.", digitalObject.getInstance()));
        }

        File[] files = parentFile.listFiles();
        if (files == null) {
            throw new AltoEditorException("No files found in temporary folder: " + parentFile.getAbsolutePath());
        }

        for (File file : files) {
            if (!file.isFile()) continue;

            String filename = file.getName();
            String uuid = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;

            try {
                if (filename.endsWith(".xml") || filename.endsWith(".txt")) {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                    if (filename.endsWith(".txt")) {
                        uploadStream(instance, userProfile.getToken(), "uuid:" + uuid, Const.DATASTREAM_TYPE_OCR, content);
                    } else if (filename.endsWith(".xml")) {
                        uploadStream(instance, userProfile.getToken(), "uuid:" + uuid, Const.DATASTREAM_TYPE_ALTO, content);
                    }
                }
            } catch (IOException e) {
                throw new AltoEditorException("Failed to read or upload file: " + filename, e);
            }
        }

        K7Indexer k7Indexer = new K7Indexer();
        k7Indexer.indexDocument(instance, userProfile.getToken(), digitalObject.getPid());

    }

    public void uploadAltoOcr(String pid, String versionId, String instanceId, UserProfile userProfile) throws IOException, AltoEditorException, InterruptedException {
        String alto = getAltoFromRepository(pid, versionId);
        if (alto == null || alto.isEmpty()) {
            throw new AltoEditorException(alto, "Alto to upload is null or empty");
        }
        String ocr = createOcrFromAlto(alto);
        if (ocr == null || ocr.isEmpty()) {
            throw new AltoEditorException(ocr, "OCR to upload is null or empty");
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        uploadStream(instance, userProfile.getToken(), pid, Const.DATASTREAM_TYPE_ALTO, alto);
        uploadStream(instance, userProfile.getToken(), pid, Const.DATASTREAM_TYPE_OCR, ocr);

        K7Indexer k7Indexer = new K7Indexer();
        k7Indexer.indexDocument(instance, userProfile.getToken(), pid);
    }

    private void uploadStream(KrameriusOptions.KrameriusInstance instance, String token, String pid, String stream, String content) throws IOException {
        String urlUploadStream = getUrlUploadStream(instance, pid, stream);
        LOGGER.info(String.format("Trying to upload %s to %s.", stream, urlUploadStream));

        HttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(urlUploadStream);

        httpPut.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpPut.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpPut.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpPut.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));
        httpPut.setHeader(new BasicHeader("Content-Type", Const.MIMETYPE_MAP.get(stream)));

        ByteArrayEntity entity = new ByteArrayEntity(content.getBytes(StandardCharsets.UTF_8));
        httpPut.setEntity(entity);

        HttpResponse response = httpClient.execute(httpPut);
        operateResponse(pid, stream, response);
    }

    private String getUrlUploadStream(KrameriusOptions.KrameriusInstance instance, String pid, String stream) {
        return Config.getKrameriusInstanceUrl(instance.getId()) + Config.getKrameriusInstanceUrlUploadStream(instance.getId()) + pid + "/akubra/updateManaged/" + stream;
    }

    private void operateResponse(String pid, String stream, HttpResponse response) throws IOException {
        if (HTTP_OK == response.getStatusLine().getStatusCode() || HTTP_CREATED == response.getStatusLine().getStatusCode() || HTTP_ACCEPTED == response.getStatusLine().getStatusCode()) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && result.isEmpty()) {
                    LOGGER.info(pid + "/" + stream + " - Uploaded to Kramerius ");
                } else {
                    LOGGER.warn("Created Importing process, but unexpected response." + result);
                    throw new IOException("Created Importing process, but unexpected response." + result);
                }
            } else {
                LOGGER.warn("Get response but entity is null");
                throw new IOException("Get response but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn(String.format("Uploading %s ended with code %s.", stream, response.getStatusLine().getStatusCode()));
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn(String.format("Uploading %s ended with code %s and reason is %s.", stream, response.getStatusLine().getStatusCode(), object.get("message")));
                    throw new IOException(String.format("Uploading %s ended with code %s and reason is %s.", stream, response.getStatusLine().getStatusCode(), object.get("message")));
                } else {
                    LOGGER.warn(String.format("Uploading %s ended with code %s and reason is null.", stream, response.getStatusLine().getStatusCode()));
                    throw new IOException(String.format("Uploading %s ended with code %s and reason is null.", stream, response.getStatusLine().getStatusCode()));
                }
            } else {
                LOGGER.warn(String.format("Uploading %s ended with code %s and entity is null.", stream, response.getStatusLine().getStatusCode()));
                throw new IOException(String.format("Uploading %s ended with code %s and entity is null.", stream, response.getStatusLine().getStatusCode()));
            }
        } else {
            LOGGER.warn(String.format("Uploading %s ended with code %s.", stream, response.getStatusLine().getStatusCode()));
            throw new IOException(String.format("Uploading %s ended with code %s.", stream, response.getStatusLine().getStatusCode()));
        }
    }

    private String getAltoFromRepository(String pid, String versionId) throws IOException, AltoEditorException {
        AkubraStorage storage = AkubraStorage.getInstance();
        AkubraStorage.AkubraObject object = storage.find(pid);
        AltoDatastreamEditor altoEditor = AltoDatastreamEditor.alto(object);
        String alto = altoEditor.readRecordAsString(versionId);
        return alto;
    }
}
