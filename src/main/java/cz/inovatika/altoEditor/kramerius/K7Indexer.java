package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_FAILED;
import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_FINISHED;
import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_PLANNED;
import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_RUNNING;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7Indexer {

    private static final Logger LOGGER = Logger.getLogger(K7Indexer.class.getName());

    public String indexDocument(KrameriusOptions.KrameriusInstance instance, String token, String pid) throws IOException, DigitalObjectException, JSONException, InterruptedException {
        Objects.requireNonNull(instance, "Instance must not be null");
        Objects.requireNonNull(token, "Token must not be null");
        Objects.requireNonNull(pid, "Pid must not be null");

        String urlUploadStream = getUrlIndex(instance);
        LOGGER.info(String.format("Trying to index %s of %s.", pid, urlUploadStream));

        String json = "{\"defid\": \"new_indexer_index_object\", \"params\": { \"type\": \"TREE_AND_FOSTER_TREES\", \"pid\": \"" + pid + "\", \"ignoreInconsistentObjects\": true } }";

        LOGGER.info("Trying to create new Kramerius process " + json + ".");

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(urlUploadStream);

        httpPost.setHeader(new BasicHeader("Connection", "keep-alive"));
        if (token != null && !token.isEmpty()) {
            httpPost.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpPost.setHeader(new BasicHeader("Content-Type", "application/json"));

        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        HttpResponse response = httpClient.execute(httpPost);

        if (HTTP_OK == response.getStatusLine().getStatusCode() || HTTP_CREATED == response.getStatusLine().getStatusCode() || HTTP_ACCEPTED == response.getStatusLine().getStatusCode()) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    String processUuid = object.getString("uuid");
                    if (processUuid == null || processUuid.isEmpty()) {
                        LOGGER.warning("Created Kramerius import success, but ProArc does not get id of this process, so state is unknown.");
                        throw new IOException("Created Kramerius import success, but ProArc does not get id of this process, so state is unknown.");
                    }
                    String state = getState(processUuid, token, instance);
                    LOGGER.info("Requesting Kramerius import success, server response is (process: " + state + ").");
                    return state;
                } else {
                    LOGGER.warning("Created Importing process, but unexpected response." + result);
                    throw new IOException("Created Importing process, but unexpected response." + result);
                }
            } else {
                LOGGER.warning("Downloaded FOXML but entity is null");
                throw new IOException("Downloaded FOXML but entity is null");
            }
        } else {
            LOGGER.warning("Importing FOXML ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Importing FOXML ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private String getUrlIndex(KrameriusOptions.KrameriusInstance instance) {
        return Config.getKrameriusInstanceUrl(instance.getId()) + Config.getKrameriusInstanceUrlParametrizedImportQuery(instance.getId());
    }

    public static String getState(String processUuid, String token, KrameriusOptions.KrameriusInstance instance) throws IOException, JSONException, InterruptedException {

        String processQueryUrl = Config.getKrameriusInstanceUrl(instance.getId()) + Config.getKrameriusInstanceUrlStateQuery(instance.getId()) + processUuid;
        LOGGER.info("Trying to get Kramerius process status " + processQueryUrl);

        String state = KRAMERIUS_PROCESS_PLANNED;
        int error403counter = 0;

        while (state.equals(KRAMERIUS_PROCESS_PLANNED) || state.equals(KRAMERIUS_PROCESS_RUNNING)) {

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(processQueryUrl);

            httpGet.setHeader(new BasicHeader("Connection", "keep-alive"));
            if (token != null && !token.isEmpty()) {
                httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
            }
            httpGet.setHeader(new BasicHeader("Content-Type", "application/json"));

            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() < HTTP_BAD_REQUEST) {
                error403counter = 0;
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JSONObject objectProcess = new JSONObject(result).getJSONObject("process");
                if (objectProcess != null) {
                    state = objectProcess.getString("state");
                } else {
                    throw new IOException("ProArc can not get state of process " + processUuid);
                }
            } else if (response.getStatusLine().getStatusCode() == HTTP_FORBIDDEN) {
                if (error403counter < 25) {
                    error403counter++;
                    TimeUnit.SECONDS.sleep(30);
                } else {
                    state = KRAMERIUS_PROCESS_FINISHED;
                    break;
                }
            } else {
                state = KRAMERIUS_PROCESS_FAILED;
                break;
            }
            if (state.equals(KRAMERIUS_PROCESS_PLANNED) || state.equals(KRAMERIUS_PROCESS_RUNNING)) {
                TimeUnit.SECONDS.sleep(20);
            }
        }
        return state;
    }
}
