package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Const;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.OcrUtils.createOcrFromAlto;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7Uploader {

    private static final Logger LOGGER = LogManager.getLogger(K7Uploader.class.getName());

    public void uploadAltoOcr(String pid, String versionId, String instanceId, UserProfile userProfile) throws IOException, AltoEditorException {
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

        StringEntity body = new StringEntity(content, StandardCharsets.UTF_8.name());
        body.setContentType(Const.MIMETYPE_MAP.get(stream));
        body.setContentEncoding(StandardCharsets.UTF_8.name());
        httpPut.setEntity(body);

        HttpResponse response = httpClient.execute(httpPut);
        operateResponse(response, stream);
    }

    private String getUrlUploadStream(KrameriusOptions.KrameriusInstance instance, String pid, String stream) {
        return Config.getKrameriusInstanceUrl(instance.getId()) + Config.getKrameriusInstanceUrlUploadStream(instance.getId()) + pid + "/akubra/updateManaged/" + stream;
    }

    private void operateResponse(HttpResponse response, String stream) throws IOException {
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug(String.format("Http response Uploaded %s success", stream));
            return;
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
