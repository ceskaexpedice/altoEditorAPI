package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.net.HttpURLConnection.HTTP_OK;

public class K7Authenticator {

    private static final Logger LOGGER = LogManager.getLogger(K7Authenticator.class.getName());

    private KrameriusOptions.KrameriusInstance instance;

    public K7Authenticator(KrameriusOptions.KrameriusInstance instance) {
        this.instance = instance;
    }

    public String authenticate() throws IOException, JSONException {
        String loginUrl = Config.getKrameriusInstanceUrl(instance.getId()) + Config.getKrameriusInstanceUrlLogin(instance.getId());

        LOGGER.info("Trying to authenticate " + loginUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(loginUrl);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", Config.getKrameriusInstanceUsername(instance.getId())));
        params.add(new BasicNameValuePair("password", Config.getKrameriusInstancePassword(instance.getId())));
        params.add(new BasicNameValuePair("client_id", Config.getKrameriusInstanceClientId(instance.getId())));
        params.add(new BasicNameValuePair("client_secret", Config.getKrameriusInstanceClientSecret(instance.getId())));
        params.add(new BasicNameValuePair("grant_type", Config.getKrameriusInstanceGrantType(instance.getId())));

        httpPost.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        httpPost.setHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        httpPost.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));

        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpResponse response = httpClient.execute(httpPost);
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(result);
                    String token = jsonObject.optString("access_token");
                    if (token != null || !token.isEmpty()) {
                        LOGGER.debug("Connected to Kramerius and get token " + token);
                        return token;
                    } else {
                        LOGGER.error("Connected to Kramerius but access_token is null");
                    }
                } else if (result.startsWith("[")){
                    JSONArray jsonArray = new JSONArray(result);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        String token = jsonObject.optString("access_token");
                        if (token != null || !token.isEmpty()) {
                            LOGGER.debug("Connected to Kramerius and get token " + token);
                            return token;
                        } else {
                            LOGGER.error("Connected to Kramerius but access_token is null");
                        }
                    }
                } else {
                    LOGGER.error("Connected to Kramerius but can not found access_token");
                    throw new IOException("Connected to Kramerius but can not found access_token");
                }
            } else {
                LOGGER.error("Connected to Kramerius but entity is null");
                throw new IOException("Connected to Kramerius but entity is null");
            }
            LOGGER.error("Connected to Kramerius but access_token is null");
            throw new IOException("Connected to Kramerius but access_token is null");
        } else {
            LOGGER.error("Connecing to Kramerius ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("Connecing to Kramerius ended with code " + response.getStatusLine().getStatusCode());
        }
    }
}
