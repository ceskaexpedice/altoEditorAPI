package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.models.ObjectInformation;
import cz.inovatika.altoEditor.utils.Config;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.Utils.getJSONArray;
import static cz.inovatika.altoEditor.utils.Utils.getJSONObject;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

public class K7ObjectInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(K7ObjectInfo.class.getName());

    public ObjectInformation getInfo(String pid, String instanceId) throws AltoEditorException, IOException {
        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        K7Authenticator authenticator = new K7Authenticator(instance);
        String token = authenticator.authenticate();

        if (token == null || token.isEmpty()) {
            LOGGER.error("Kramerius token is null");
            throw new AltoEditorException(instanceId, "Kramerius token is null");
        }

        String objectInformationUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=pid:" +
                URLEncoder.encode("\"" + pid + "\"", StandardCharsets.UTF_8.name()) + "&wt=xml";
        LOGGER.info("Trying to get Object info " + objectInformationUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(objectInformationUrl);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        HttpResponse response = httpClient.execute(httpGet);

        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Object information success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return getObjectInformation(pid, result);
                } else {
                    LOGGER.warn("GET Object information but result is null or empty");
                    throw new IOException("GET Object information but result is null or empty");
                }
            } else {
                LOGGER.warn("GET Object information but entity is null");
                throw new IOException("GET Object information but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("GETTING Object information ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("GET Object information ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("GETTING Object information ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("GETTING Object information ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private ObjectInformation getObjectInformation(String pid, String objectInformation) {
        String label = null;
        String parentLabel = null;
        String parentPid = null;

        JSONObject objectInformationJSON = XML.toJSONObject(objectInformation);
        JSONObject responseJSON = objectInformationJSON.getJSONObject("response");
        JSONObject resultJSON = responseJSON.getJSONObject("result");
        JSONObject docJSON = resultJSON.getJSONObject("doc");

//        JSONArray arrArray = docJSON.getJSONArray("arr");
        JSONArray strArray = docJSON.getJSONArray("str");

//        for (int i = 0; i < arrArray.length(); i++) {
//            JSONObject arrJSON = arrArray.getJSONObject(i);
//            String name = arrJSON.getString("name");
//            if ("title.search".equals(name)) {
//                label = arrJSON.getString("str");
//            }
//            if ("own_parent.title".equals(name)) {
//                parentLabel = arrJSON.getString("str");
//            }
//            if ("own_pid_path".equals(name)) {
//                parentPid = arrJSON.getString("str");
//            }
//        }

        for (int i = 0; i < strArray.length(); i++) {
            JSONObject strJSON = strArray.getJSONObject(i);
            String name = strJSON.getString("name");
            if ("title.search".equals(name)) {
                label = transformValue(strJSON.get("content"));
            }
            if ("own_parent.title".equals(name)) {
                parentLabel = transformValue(strJSON.getString("content"));
            }
            if ("own_pid_path".equals(name)) {
                parentPid = transformValue(strJSON.getString("content"));
            }
        }
        return new ObjectInformation(pid, label, parentPid, parentLabel);
    }

    private String transformValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return String.valueOf(value);
        } else {
            return value.toString();
        }
    }

    public String getModel(String pid, String instanceId) throws AltoEditorException, IOException {

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        K7Authenticator authenticator = new K7Authenticator(instance);
        String token = authenticator.authenticate();

        if (token == null || token.isEmpty()) {
            LOGGER.error("Kramerius token is null");
            throw new AltoEditorException(instanceId, "Kramerius token is null");
        }

        String modelInfoUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=pid:" +
                URLEncoder.encode("\"" + pid + "\"", StandardCharsets.UTF_8.name()) + "&fl=model";
        LOGGER.info("Trying to get Model info " + modelInfoUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(modelInfoUrl);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        HttpResponse response = httpClient.execute(httpGet);
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Model info success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return getModelInfo(result);
                } else {
                    LOGGER.warn("GET Model info but result is null or empty");
                    throw new IOException("GET Model info but result is null or empty");
                }
            } else {
                LOGGER.warn("GET Model info but entity is null");
                throw new IOException("GET Model info but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("GETTING Model info ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("GET Model info ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("GETTING Model info ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("GETTING Model info ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    public List<String> getChildrenPids(String pid, String instanceId) throws AltoEditorException, IOException {
        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        K7Authenticator authenticator = new K7Authenticator(instance);
        String token = authenticator.authenticate();

        if (token == null || token.isEmpty()) {
            LOGGER.error("Kramerius token is null");
            throw new AltoEditorException(instanceId, "Kramerius token is null");
        }

        String childrenInfoUrl = Config.getKrameriusInstanceUrl(instance.getId()) +
                Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=own_parent.pid:" +
                URLEncoder.encode("\"" + pid + "\"", StandardCharsets.UTF_8.name()) + "&fl=pid";
        LOGGER.info("Trying to get children info " + childrenInfoUrl);

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(childrenInfoUrl);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        HttpResponse response = httpClient.execute(httpGet);
        if (HTTP_OK == response.getStatusLine().getStatusCode()) {
            LOGGER.debug("Http response Model info success");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (result != null && !result.isEmpty()) {
                    return getChildrenPids(result);
                } else {
                    LOGGER.warn("GET Model info but result is null or empty");
                    throw new IOException("GET Model info but result is null or empty");
                }
            } else {
                LOGGER.warn("GET Children info but entity is null");
                throw new IOException("GET Children info but entity is null");
            }
        } else if (HTTP_INTERNAL_ERROR == response.getStatusLine().getStatusCode()) {
            LOGGER.warn("GETTING Children info ended with code " + response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(response.getEntity());
                if (result != null && !result.isEmpty()) {
                    JSONObject object = new JSONObject(result);
                    LOGGER.warn("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                    throw new IOException("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and reason is " + object.get("message"));
                } else {
                    LOGGER.warn("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                    throw new IOException("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and the result is null");
                }
            } else {
                LOGGER.warn("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
                throw new IOException("GET Children info ended with code " + response.getStatusLine().getStatusCode() + " and the entity is null");
            }
        } else {
            LOGGER.warn("GETTING Children info ended with code " + response.getStatusLine().getStatusCode());
            throw new IOException("GETTING Children info ended with code " + response.getStatusLine().getStatusCode());
        }
    }

    private List<String> getChildrenPids(String result) {
        List<String> pids = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(result);
            jsonObject = getJSONObject(jsonObject, "response");
            JSONArray array = getJSONArray(jsonObject, "docs");
            for (int i = 0; i < array.length(); i++) {
                jsonObject = array.getJSONObject(i);
                if (jsonObject != null) {
                    pids.add(jsonObject.getString("pid"));
                }
            }
            return pids;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private String getModelInfo(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            jsonObject = getJSONObject(jsonObject, "response");
            JSONArray array = getJSONArray(jsonObject, "docs");
            if (array.length() > 0) {
                jsonObject = array.getJSONObject(0);
                if (jsonObject != null) {
                    return jsonObject.getString("model");
                }
            }
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
