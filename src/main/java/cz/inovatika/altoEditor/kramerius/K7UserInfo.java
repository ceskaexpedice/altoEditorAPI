package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.user.UserProfile;
import java.io.IOException;
import java.util.ArrayList;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.net.HttpURLConnection.HTTP_OK;

public class K7UserInfo {

    private static final Logger LOGGER = LogManager.getLogger(K7UserInfo.class.getName());

    private KrameriusOptions.KrameriusInstance instance;

    public K7UserInfo() {
    }

    public UserProfile getUser(String token) throws IOException, JSONException {
        String userInfoUrl = Config.getKeycloakUrl() + Config.getKeycloakUserInfo();

        LOGGER.debug("Trying to get user info " + userInfoUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(userInfoUrl);
        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        HttpResponse response = httpClient.execute(httpGet);

        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(result);
                    String username = jsonObject.optString("uid");
                    JSONArray roleJSONArray = jsonObject.getJSONArray("roles");
                    List<String> roles = new ArrayList<>();
                    for (int i = 0; i < roleJSONArray.length(); i++) {
                        roles.add(roleJSONArray.getString(i));
                    }
                    if (username != null && !username.isEmpty()) {
                        LOGGER.info("Connected to Kramerius and get user info " + username);
                        UserProfile userProfile = new UserProfile(username, token, roles);
                        return userProfile;
                    } else {
                        LOGGER.error("Connected to Kramerius but user info dont get");
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
