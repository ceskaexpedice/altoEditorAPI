package cz.inovatika.altoEditor.process;

import cz.inovatika.altoEditor.utils.Config;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The PeroOcrProcessor class is responsible for handling OCR (Optical Character Recognition)
 * operations using the Pero OCR API. It provides methods for processing images, generating OCR outputs,
 * and managing communication with the Pero OCR server.
 */
public class PeroOcrProcessor {

    private static final Logger LOGGER = LogManager.getLogger(PeroOcrProcessor.class.getName());

    private String apiKey;
    private String serverUrl;
    private int peroOcrEngine;

    private String imagePath;
    private String txtPath;
    private String altoPath;

    public PeroOcrProcessor() {
        this.serverUrl = Config.getProcessorPeroUrl();
        this.apiKey = Config.getProcessorPeroKey();
    }

    public PeroOcrProcessor(Integer peroOcrEngine) {
        this.serverUrl = Config.getProcessorPeroUrl();
        this.apiKey = Config.getProcessorPeroKey();
        this.peroOcrEngine = (peroOcrEngine == null || peroOcrEngine < 1) ? 1 : peroOcrEngine;
    }

    public boolean generate(File imageFile, String ocrFileSuffix, String altoFileSuffix) throws JSONException {
        this.imagePath = imageFile.getAbsolutePath();

        File[] outputFiles = getOcrFiles(imageFile, ocrFileSuffix, altoFileSuffix);
        this.txtPath = outputFiles[0].getAbsolutePath();
        this.altoPath = outputFiles[1].getAbsolutePath();

        return process(this.imagePath, this.txtPath, this.altoPath);
    }

    public static File[] getOcrFiles(File imageFile, String plainOcrFileSuffix, String altoFileSuffix) {
        String basePath = imageFile.getAbsolutePath();
        int dotIndex = basePath.lastIndexOf('.');
        if (dotIndex > 0) {
            basePath = basePath.substring(0, dotIndex);
        }

        File txtFile = new File(basePath + plainOcrFileSuffix);
        File altoFile = new File(basePath + altoFileSuffix);

        return new File[]{txtFile, altoFile};
    }

    public boolean process(String imagePath, String txtPath, String altoPath) throws JSONException {
        String fileName = new File(imagePath).getName();
        String fileExtension = null;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex + 1);
            fileName = fileName.substring(0, dotIndex);
        }
        if (fileExtension == null) {
            return false;
        }

        String contentType = getContentType(fileExtension);

        JSONObject data = createJson(fileName);
        String txtFormat = "txt";
        String altoFormat = "alto";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String requestId = postProcessingRequest(httpClient, data);
            if (requestId == null) {
                return false;
            }

            boolean uploaded = uploadImage(httpClient, requestId, fileName, imagePath, contentType);
            if (!uploaded) {
                LOGGER.error("Image upload failed for {}", fileName);
                return false;
            }

            String processingResult;
            do {
                processingResult = downloadResults(httpClient, txtPath, requestId, fileName, txtFormat);
                if (Objects.equals("PROCESSED", processingResult)) {
                    downloadResults(httpClient, altoPath, requestId, fileName, altoFormat);
                    LOGGER.info("Files {} and {} created for {}", txtFormat, altoFormat, fileName);
                } else {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        LOGGER.warn("Task interrupted during sleep for {}", fileName);
                    }
                    if (Thread.interrupted()) {
                        LOGGER.info("Thread stopped for {}", fileName);
                        return false;
                    }
                }
                if (processingResult == null) {
                    LOGGER.error("Processing result is null for request {}", requestId);
                    return false;
                }
            } while (!Objects.equals("PROCESSED", processingResult));
            return true;
        } catch (IOException e) {
            LOGGER.error("Exception during processing of {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    private JSONObject createJson(String fileName) throws JSONException {
        JSONObject outputData = new JSONObject();
        outputData.put(fileName, JSONObject.NULL);
        JSONObject dataDict = new JSONObject();
        dataDict.put("engine", peroOcrEngine);
        dataDict.put("images", outputData);
        return dataDict;
    }

    private String postProcessingRequest(CloseableHttpClient httpClient, JSONObject data) {
        String url = this.serverUrl + "post_processing_request";
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("api-key", this.apiKey);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(data.toString(), "UTF-8"));


        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();

            if (entity == null) {
                LOGGER.error("Post processing request returned empty response.");
                return "ERROR";
            }

            if (statusCode >= 200 && statusCode < 400) {
                JSONObject jsonResponse = new JSONObject(EntityUtils.toString(entity, StandardCharsets.UTF_8));

                while (!jsonResponse.getString("status").equals("success")) {

                    TimeUnit.SECONDS.sleep(15);

                    String requestId = jsonResponse.optString("request_id", null);
                    if (requestId == null) {
                        LOGGER.error("No request_id returned from post processing request.");
                        return null;
                    }

                    jsonResponse = checkStatus(httpClient, jsonResponse.getString("request_id"));
                }
                return jsonResponse.getString("request_id");
            } else {
                LOGGER.error( "The post processing request ended with status code {}.", statusCode);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return null;
    }

    private JSONObject checkStatus(CloseableHttpClient httpClient, String requestId) throws IOException, JSONException {
        String url = this.serverUrl + "get_status?request_id=" + requestId;
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("api-key", this.apiKey);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return new JSONObject(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            } else {
                LOGGER.error("Failed to get status for request {}. HTTP code: {}", requestId, statusCode);
                throw new IOException("Unexpected code " + statusCode);
            }
        }
    }

    private boolean uploadImage(CloseableHttpClient httpClient, String requestId, String fileName, String imagePath, String contentType) {
        String url = this.serverUrl + "upload_image/" + requestId + "/" + fileName;
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("api-key", this.apiKey);

        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            LOGGER.error("File {} does not exist or is not a file", imagePath);
            return false;
        }

        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageFile);
        httpPost.setEntity(builder.build());

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 200 && statusCode < 300) {
                LOGGER.info("Image upload of {} succeeded with status code {}", fileName, statusCode);
                return true;
            } else {
                LOGGER.error("Image upload of {} failed with status {}. Response: {}", fileName, statusCode, body);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during image upload of " + fileName, e);
            return false;
        }
    }

    private String downloadResults(CloseableHttpClient httpClient, String outputPath, String requestId, String fileName, String resultFormat) {
        String url = this.serverUrl + "download_results/" + requestId + "/" + fileName + "/" + resultFormat;
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("api-key", this.apiKey);

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();

            int statusCode = response.getStatusLine().getStatusCode();


            if (statusCode >= 200 && statusCode < 300) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                    return "PROCESSED";
                }
            } else if (statusCode >= 400) {
                JSONObject jsonResponse = new JSONObject(EntityUtils.toString(entity, StandardCharsets.UTF_8));

                if (jsonResponse.getString("message").contains("not processed yet")) {
                    return "UNPROCESSED";
                } else {
                    LOGGER.info("Request returned status {}: {}", statusCode, jsonResponse.getString("message"));
                }
            }
        } catch (JSONException | IOException e) {
            LOGGER.error("Error during downloading PERO results for request {}", requestId, e);
        }
        return "ERROR";
    }

    private String getContentType(String fileExtension) {
        if (fileExtension == null) {
            LOGGER.error("File extension is null");
            return "application/octet-stream";
        }

        switch (fileExtension.toLowerCase()) {
            case "tiff":
            case "tif":
                return "image/tiff";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "jp2":
                return "image/jp2";
            default:
                LOGGER.error("Unsupported file extension: {}", fileExtension);
                return "application/octet-stream"; // fallback
        }
    }

    public List<PeroOcrEnginesDescriptor> getEnginesList() throws IOException {
        List<PeroOcrEnginesDescriptor> peroOcrEngines = new ArrayList<>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            JSONObject response = getEngines(httpClient);

            if (!response.has("engines") || response.isNull("engines")) {
                throw new IOException("Missing required object 'engines' in response");
            }

            JSONObject engines = response.getJSONObject("engines");

            for (Iterator<String> it = engines.keys(); it.hasNext(); ) {
                String label = it.next();
                JSONObject engine = engines.getJSONObject(label);

                int id = engine.getInt("id");
                String description = engine.optString("description", null);

                peroOcrEngines.add(
                        new PeroOcrEnginesDescriptor(id, label, description)
                );
            }
        }

        peroOcrEngines.sort(Comparator.comparingInt(PeroOcrEnginesDescriptor::getId));

        return peroOcrEngines;
    }

    private JSONObject getEngines(CloseableHttpClient httpClient) throws IOException, JSONException {
        String url = this.serverUrl + "get_engines";
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("api-key", this.apiKey);
        httpGet.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new IOException("Response entity is null");
                }
                return new JSONObject(EntityUtils.toString(entity, StandardCharsets.UTF_8));
            } else {
                throw new IOException("Unexpected HTTP Status " + statusCode);
            }
        }
    }

    public class PeroOcrEnginesDescriptor {

        private int id;
        private String label;
        private String description;

        public PeroOcrEnginesDescriptor(int id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}