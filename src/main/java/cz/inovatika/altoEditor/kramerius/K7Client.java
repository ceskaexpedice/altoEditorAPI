package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.models.ObjectInformation;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Const;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_PLANNED;
import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.KRAMERIUS_PROCESS_RUNNING;
import static cz.inovatika.altoEditor.utils.OcrUtils.transformJsonValue;

/**
 * Represents a client for interacting with a Kramerius v7 server. This client provides
 * functionalities for downloading, uploading, and retrieving metadata and content related
 * to digital objects stored on the server.
 */
public class K7Client extends AbstractHttpClient {

    private static final Logger LOGGER = LogManager.getLogger(K7Client.class);

    private KrameriusOptions.KrameriusInstance instance;

    public K7Client(KrameriusOptions.KrameriusInstance instance) {
        super(Config.getKrameriusInstanceUrl(instance.getId()));
        this.instance = instance;
    }

    public String downloadFoxml(String pid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlDownloadFoxml(this.instance.getId()) + pid + "/foxml", token);

        return execute(request, body -> body);
    }

    public String downloadAlto(String pid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlDownloadFoxml(instance.getId()) + pid + "/ocr/alto", token);

        return execute(request, body -> body);
    }

    public HttpResponse getResponse(String pid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlImage(instance.getId()) + pid + "/image", token);

        return execute(request, body -> {return httpClient.execute(request);});
    }

    public InputStream getStream(String pid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlImage(instance.getId()) + pid + "/image", token);

        return executeForStream(request);
    }

    public ObjectInformation getObjectInformation(String pid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=pid:" + URLEncoder.encode("\"" + pid + "\"", StandardCharsets.UTF_8.name()) + "&wt=xml", token);

        return execute(request, body -> getObjectInformationFromResponse(pid, body));
    }

    public List<String> getChildrenPids(String parentPid, String token, int start, int rows) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=own_parent.pid:" +
                URLEncoder.encode("\"" + parentPid + "\"", StandardCharsets.UTF_8.name()) + "&fl=pid,model&rows=" + rows + "&start=" + start, token);

        return execute(request, body -> {return parseChildren(body, token);});
    }

    public int getChildrenSize(String parentPid, String token) throws IOException {

        HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlModelInfo(instance.getId()) + "?q=own_parent.pid:" +
                URLEncoder.encode("\"" + parentPid + "\"", StandardCharsets.UTF_8) + "&fl=pid,model", token);

        return execute(request, this::parseChildrenSize);
    }

    public void uploadStream(String pid, String stream, String content, String token) throws IOException {

        HttpPut request = createHttpPut(baseUrl + Config.getKrameriusInstanceUrlUploadStream(instance.getId()) + pid + "/akubra/updateManaged/" + stream, token, Const.MIMETYPE_MAP.get(stream));

        ByteArrayEntity entity = new ByteArrayEntity(content.getBytes(StandardCharsets.UTF_8));
        request.setEntity(entity);

        execute(request, null);
    }

    public String indexDocument(String pid, String token) throws IOException, InterruptedException {

        HttpPost request = createHttpPost(baseUrl + Config.getKrameriusInstanceUrlParametrizedImportQuery(instance.getId()), token);

        String json = """
        {
          "defid": "new_indexer_index_object",
          "params": {
            "type": "TREE_AND_FOSTER_TREES",
            "pid": "%s",
            "ignoreInconsistentObjects": true
          }
        }
        """.formatted(pid);

        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        String processId = execute(request, body ->
                new JSONObject(body).getString("uuid")
        );

        return waitForProcess(processId, token);
    }

    public List<String> getChildrenPids(String pid, String token) throws IOException {


        List<String> allPids = new ArrayList<>();
        int rows = 1000;
        int start = 0;

        int childCount = getChildrenSize(pid, token);

        while (start < childCount) {
            int currentRows = Math.min(rows, childCount - start);
            allPids.addAll(getChildrenPids(pid, token, start, currentRows));
            start += currentRows;
        }

        return allPids;
    }

    private int parseChildrenSize(String body) {

        if (body == null || body.isBlank()) {
            return 0;
        }

        try {
            JSONObject response = new JSONObject(body).optJSONObject("response");

            return response != null ? Math.max(response.optInt("numFound", 0), 0) : 0;
        } catch (Exception e) {
            LOGGER.error("Failed to parse children PIDs", e);
            return 0;
        }
    }

    private List<String> parseChildren(String body, String token) {

        List<String> pids = new ArrayList<>();

        if (body == null || body.isBlank()) {
            return pids;
        }

        try {
            JSONObject root = new JSONObject(body);
            JSONObject response = root.optJSONObject("response");
            if (response == null) {
                return pids;
            }

            JSONArray docs = response.optJSONArray("docs");
            if (docs == null) {
                return pids;
            }

            for (int i = 0; i < docs.length(); i++) {
                JSONObject doc = docs.optJSONObject(i);
                if (doc == null) {
                    continue;
                }

                String pid = doc.optString("pid", null);
                String model = doc.optString("model", null);
                if (pid != null && !pid.isBlank()) {
                    if (model != null && !model.isBlank()) {
                        if ("page".equalsIgnoreCase(model)) {
                            pids.add(pid);
                        } else {
                            pids.addAll(getChildrenPids(pid, token));
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to parse children PIDs", e);
        }

        return pids;
    }

    private ObjectInformation getObjectInformationFromResponse(String pid, String xmlBody) {
        try {

            JSONObject docJson = XML.toJSONObject(xmlBody)
                    .getJSONObject("response")
                    .getJSONObject("result")
                    .getJSONObject("doc");

            JSONArray fields = docJson.optJSONArray("str");
            if (fields == null) {
                LOGGER.warn("Missing 'str' array in response for pid {}", pid);
                return new ObjectInformation(pid, null, null, null, null, null);
            }

            String model = null;
            String label = null;
            String parentLabel = null;
            String parentPid = null;
            String parentPath = null;

            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String name = field.optString("name");

                Object content = field.opt("content");
                if (content == null) {
                    continue;
                }

                switch (name) {
                    case "model":
                        model = transformJsonValue(content);
                        break;
                    case "title.search":
                        label = transformJsonValue(content);
                        break;
                    case "own_parent.title":
                        parentLabel = transformJsonValue(content);
                        break;
                    case "own_pid_path":
                        parentPath = transformJsonValue(content);
                        break;
                    case "own_parent.pid":
                        parentPid = transformJsonValue(content);
                        break;
                }
            }

            return new ObjectInformation(pid, model, label, parentPath, parentLabel, parentPid);

        } catch (JSONException ex) {
            LOGGER.warn("Failed to parse object information for pid {}. Returning minimal object.", pid, ex);
            return new ObjectInformation(pid, null, null, null, null, null);
        }
    }

    private String waitForProcess(String processUuid, String token)
            throws IOException, InterruptedException {

        String state;

        do {
            HttpGet request = createHttpGet(baseUrl + Config.getKrameriusInstanceUrlStateQuery(instance.getId()) + processUuid, token);

            state = execute(request, body ->
                    new JSONObject(body)
                            .getJSONObject("process")
                            .getString("state")
            );

            if (KRAMERIUS_PROCESS_PLANNED.equals(state) || KRAMERIUS_PROCESS_RUNNING.equals(state)) {
                Thread.sleep(20000);
            }

        } while (KRAMERIUS_PROCESS_PLANNED.equals(state) || KRAMERIUS_PROCESS_RUNNING.equals(state));

        return state;
    }
}
