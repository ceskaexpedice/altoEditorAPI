package cz.inovatika.altoEditor.utils;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.response.AltoEditorStringRecordResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.apache.http.HttpResponse;
import org.codehaus.jackson.node.ArrayNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Const.APP_HOME;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class.getName());

    public static File getDefaultHome() {
        File userHome = new File(System.getProperty("user.home"));
        return new File(userHome, APP_HOME);
    }

    public static void setContext(Context ctx, String contentType) {
        ctx.header("Cache-Control", "must-revalidate,no-cache,no-store");
        ctx.res().setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (contentType != null && !contentType.isEmpty()) {
            ctx.contentType(contentType);
        }
    }

    public static void setContext(Context ctx) {
        setContext(ctx, "text/html; charset=utf-8");
    }

    public static void setResult(Context ctx, String message){
        try {
            InputStream stream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8.name()));
            ctx.result(stream);
        } catch (UnsupportedEncodingException ex) {
            ctx.result(ex.getMessage());
            LOGGER.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void setResult(Context ctx, HttpResponse response, String pid) {
        try {
            ctx.result(response.getEntity().getContent());
            ctx.header("Content-Type", response.getFirstHeader("Content-Type").getValue());
            ctx.header("Content-Disposition", pid + ".jpg");
        } catch (IOException ex) {
            ctx.result(ex.getMessage());
            LOGGER.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void setResult(Context ctx, AltoEditorResponse altoEditorResponse) {
        String json = new Gson().toJson(altoEditorResponse);
        setResult(ctx, json);
    }

    public static void setStringResult(Context ctx, AltoEditorStringRecordResponse altoEditorResponse) {
        String json = new Gson().toJson(altoEditorResponse);
        setResult(ctx, json);
    }

    public static String getPath(String root, String endpoint) {
        if (!root.endsWith("/")) {
            root += "/";
        }
        return root + endpoint;
    }


    public static void closeSilently(Object object) {
        if (object != null) {
            try {
                if (object instanceof InputStream) {
                    ((InputStream) object).close();
                } else if (object instanceof OutputStream) {
                    ((OutputStream) object).close();
                } else if (object instanceof Connection) {
                    ((Connection) object).close();
                } else if (object instanceof Statement) {
                    ((Statement) object).close();
                } else {
                    LOGGER.warn("Unknown object to close");
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to close the stream.");
                e.printStackTrace();
            } catch (SQLException e) {
                LOGGER.warn("Unable to close the db connection.");
                e.printStackTrace();
            }
        }
    }

    public static JSONArray getJSONArray(JSONObject object, String key) {
        if (object == null) {
            return null;
        }
        return object.getJSONArray(key);
    }

    public static JSONObject getJSONObject(JSONObject object, String key) {
        if (object == null) {
            return null;
        }
        return object.getJSONObject(key);
    }

    public static String getStringNodeValue(JsonNode node, String key) throws AltoEditorException {
        if (node.get(key) != null) {
            JsonNode keyNode = node.get(key);
            if (keyNode.textValue() == null || keyNode.textValue().isEmpty()) {
                throw new RequestException(key, String.format("Missing value of param \"%s\".", key));
            } else {
                return keyNode.textValue();
            }
        } else {
            throw new RequestException(key, String.format("Missing value of param \"%s\".", key));
        }
    }

    public static String getOptStringNodeValue(JsonNode node, String key) throws AltoEditorException {
        if (node.get(key) != null) {
            JsonNode keyNode = node.get(key);
            return keyNode.textValue();
        } else {
            return null;
        }
    }

    public static Boolean getBooleanNodeValue(JsonNode node, String key) throws AltoEditorException {
        if (node.get(key) != null) {
            JsonNode keyNode = node.get(key);
            return keyNode.booleanValue();
        } else {
            throw new RequestException(key, String.format("Missing value of param \"%s\".", key));
        }
    }

    public static String getStringRequestValue(Context context, String key) throws RequestException {
        if (context != null) {
            if (context.req() != null) {
                String value = context.req().getParameter(key);
                if (value == null || value.isEmpty()) {
                    throw new RequestException(key, String.format("Missing param \"%s\".", key));
                } else {
                    return value;
                }
            } else {
                throw new RequestException(key, "Request in Context is null");
            }
        } else {
            throw new RequestException(key, "Context is null");
        }
    }

    public static String getOptStringRequestValue(Context context, String key) throws RequestException {
        if (context != null) {
            if (context.req() != null) {
                String value = context.req().getParameter(key);
                return value;
            } else {
                throw new RequestException(key, "Request in Context is null");
            }
        } else {
            throw new RequestException(key, "Context is null");
        }
    }

    public static InputStream readFile(String resource) throws IOException {
        Enumeration<URL> resources = AltoEditorInitializer.class.getClassLoader().getResources(resource);
        URL lastResource = null;
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            lastResource = url;
            LOGGER.debug(url.toExternalForm());
        }

        if (lastResource == null) {
            throw new IllegalStateException(resource);
        }
        return lastResource.openStream();
    }

    public static boolean checkFile(File file, Boolean mustExists, Boolean expectDirectory, Boolean expectCanRead, Boolean expectCanWrite) throws IOException {
        if (file.exists()) {
            if (expectDirectory != null) {
                if (expectDirectory && !file.isDirectory()) {
                    throw new IOException(String.format("Not a folder: '%s'!", file));
                } else if (!expectDirectory && file.isDirectory()) {
                    throw new IOException(String.format("Not a file: '%s'!", file));
                }
            }
            if (expectCanRead != null) {
                if (expectCanRead != file.canRead()) {
                    throw new IOException(String.format("Invalid read permission (=%s) for: '%s'!", !expectCanRead, file));
                }
            }
            if (expectCanWrite != null) {
                if (expectCanWrite != file.canWrite()) {
                    throw new IOException(String.format("Invalid write permission (=%s) for: '%s'!", !expectCanWrite, file));
                }
            }
            return true;
        } else if (mustExists) {
            if (expectDirectory != null && expectDirectory) {
                throw new FileNotFoundException(String.format("Folder '%s' not found!."));
            } else {
                throw new FileNotFoundException(String.format("File '%s' not found!."));
            }
        }
        return false;
    }

    public static String normalizePath(String path) {
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("./") || path.startsWith(".\\\\")) {
                path = getDefaultHome() + path.substring(1);
            }
        }
        return path;
    }

    public static String getNextDate(String currentDate) throws ParseException {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final Date date = format.parse(currentDate);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return format.format(calendar.getTime());
    }
}
