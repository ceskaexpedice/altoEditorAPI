package cz.inovatika.altoEditor.kramerius;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Abstract base class for making HTTP client requests. This class provides common functionality
 * for executing HTTP requests, handling responses, and parsing server responses. It utilizes
 * Apache HttpClient for managing HTTP connections.
 *
 * Key responsibilities:
 * - Facilitates HTTP method executions (GET, POST, PUT) and response handling.
 * - Simplifies working with response streams and parsing responses.
 * - Centralizes header management for HTTP requests.
 * - Ensures proper resource management by implementing {@link Closeable}.
 *
 * Subclasses of this class can extend its functionality to implement specific HTTP client needs.
 */
public abstract class AbstractHttpClient implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(AbstractHttpClient.class);

    protected final CloseableHttpClient httpClient;
    protected final String baseUrl;

    protected AbstractHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClients.custom()
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
                .build();
    }

    @FunctionalInterface
    protected interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }

    protected <T> T execute(HttpUriRequest request, CheckedFunction<String, T> parser) throws IOException {
        LOGGER.debug("Executing HTTP request: {} {}", request.getMethod(), request.getURI());

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            int statusCode = response.getStatusLine().getStatusCode();

            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                    : null;

            if (statusCode < HTTP_OK || statusCode >= HTTP_MULT_CHOICE) {
                LOGGER.error("HTTP error {}: {}", statusCode, extractError(body));
                throw new IOException("HTTP " + statusCode + ": " + extractError(body));
            }

            if (parser == null) {
                // PUT a DELETE nemusi nic vracet, proto jen logovat a vracet
                LOGGER.debug("No parser provided, returning null (empty body is OK for void request)");
                return null;
            }

            if (body == null || body.isBlank()) {
                LOGGER.warn("Response body is empty for request: {} {}", request.getMethod(), request.getURI());
                throw new IOException("Response body is empty");
            }

            return parser.apply(body);
        }
    }

    protected InputStream executeForStream(HttpUriRequest request) throws IOException {
        LOGGER.debug("Executing HTTP request for InputStream: {} {}", request.getMethod(), request.getURI());

        CloseableHttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();

        if (statusCode < HTTP_OK || statusCode >= HTTP_MULT_CHOICE) {
            String message = (entity != null) ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "Empty response";
            response.close();
            LOGGER.error("HTTP error {}: {}", statusCode, message);
            throw new IOException("HTTP " + statusCode + ": " + message);
        }

        if (entity == null) {
            response.close();
            LOGGER.warn("Response entity is null for request: {} {}", request.getMethod(), request.getURI());
            throw new IOException("Response entity is null");
        }

        // Získáme InputStream jen jednou
        InputStream contentStream = entity.getContent();

        return new FilterInputStream(contentStream) {
            @Override
            public void close() throws IOException {
                super.close();
                response.close();
            }
        };
    }

    private String extractError(String body) {
        if (body == null || body.isBlank()) return "Empty response";
        try {
            return new JSONObject(body).optString("message", body);
        } catch (Exception e) {
            return body;
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    public static HttpGet createHttpGet(String url, String token) {
        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        httpGet.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpGet.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        if (token != null && !token.isEmpty()) {
            httpGet.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }

        return httpGet;
    }

    protected HttpPut createHttpPut(String url, String token, String contentType) {
        HttpPut httpPut = new HttpPut(url);

        httpPut.setHeader(new BasicHeader("Keep-Alive", "timeout=600, max=1000"));
        httpPut.setHeader(new BasicHeader("Connection", "Keep-Alive, Upgrade"));
        httpPut.setHeader(new BasicHeader("Accept-Language", "cs,en;q=0.9,de;q=0.8,cs-CZ;q=0.7,sk;q=0.6"));

        if (token != null && !token.isEmpty()) {
            httpPut.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }

        if (contentType != null && !contentType.isEmpty()) {
            httpPut.setHeader(new BasicHeader("Content-Type", contentType));
        }

        return httpPut;
    }

    protected HttpPost createHttpPost(String url, String token) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(new BasicHeader("Connection", "keep-alive"));
        httpPost.setHeader(new BasicHeader("Content-Type", "application/json"));

        if (token != null && !token.isEmpty()) {
            httpPost.setHeader(new BasicHeader("Authorization", "Bearer " + token));
        }

        return httpPost;
    }

}
