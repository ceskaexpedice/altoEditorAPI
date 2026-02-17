package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.HttpGet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A client for interacting with a Keycloak server, extending the functionality of {@code AbstractHttpClient}.
 * This client provides methods to retrieve user information from Keycloak given a valid access token.
 */
public class KeyCloakClient extends AbstractHttpClient{

    private static final Logger LOGGER = LogManager.getLogger(KeyCloakClient.class);

    public KeyCloakClient(String baseUrl) {
        super(baseUrl);
    }

    public UserProfile getUser(String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKeycloakUserInfo(), token);

        return execute(request, body -> {

            JSONObject json = new JSONObject(body);

            String username = json.optString("uid");
            if (username == null || username.isBlank()) {
                LOGGER.error("User info does not contain valid uid");
                throw new IOException("User info does not contain uid");
            }

            JSONArray roleArray = json.optJSONArray("roles");

            List<String> roles = new ArrayList<>();
            if (roleArray != null) {
                for (int i = 0; i < roleArray.length(); i++) {
                    roles.add(roleArray.getString(i));
                }
            }

            return new UserProfile(username, token, roles);
        });
    }
}
