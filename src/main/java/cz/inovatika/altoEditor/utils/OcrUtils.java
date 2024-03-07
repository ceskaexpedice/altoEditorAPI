package cz.inovatika.altoEditor.utils;

import java.io.StringWriter;
import java.math.BigDecimal;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class OcrUtils {

    public static String createOcrFromAlto(String alto) {
        JSONObject jsonObject = XML.toJSONObject(alto);
        JSONObject altoJson = jsonObject.getJSONObject("alto");
        JSONObject layoutJson = altoJson.getJSONObject("Layout");
        JSONObject pageJson = layoutJson.getJSONObject("Page");
        JSONObject printSpaceJson = pageJson.getJSONObject("PrintSpace");

        String value = processPrintSpaceJSON(printSpaceJson);
        value = value.replaceAll("\n\n", "\n");
        return value.trim();
    }

    private static String processPrintSpaceJSON(JSONObject printSpaceJson) {
        StringWriter writer = new StringWriter();
        try {
            JSONArray textBlockArray = printSpaceJson.getJSONArray("TextBlock");
            for (int i = 0; i < textBlockArray.length(); i++) {
                JSONObject textBlockJson = textBlockArray.getJSONObject(i);
                String value = processTextBlockJSON(textBlockJson);
                if (value != null) {
                    writer.append(value).append("\n\n");
                }
            }
        } catch (JSONException ex) {
            if (ex.getMessage().contains("is not a JSONArray")) {
                JSONObject textBlockJson = printSpaceJson.getJSONObject("TextBlock");
                String value = processTextBlockJSON(textBlockJson);
                if (value != null && !value.isBlank()) {
                    writer.append(value.trim()).append("\n\n");
                }
            } else {
                throw ex;
            }
        }
        return writer.toString().trim();
    }

    private static String processTextBlockJSON(JSONObject textBlockJson) {
        StringWriter writer = new StringWriter();
        try {
            JSONArray textLineArray = textBlockJson.getJSONArray("TextLine");
            for (int i = 0; i < textLineArray.length(); i++) {
                JSONObject textLineJson = textLineArray.getJSONObject(i);
                String value = processTextLineJSON(textLineJson);
                if (value != null) {
                    writer.append(value).append("\n");
                }
            }
        } catch (JSONException ex) {
            if (ex.getMessage().contains("is not a JSONArray")) {
                JSONObject textLineJson = textBlockJson.getJSONObject("TextLine");
                String value = processTextLineJSON(textLineJson);
                if (value != null && !value.isBlank()) {
                    writer.append(value.trim()).append("\n");
                }
            } else {
                throw ex;
            }
        }
        return writer.toString().trim();
    }

    private static String processTextLineJSON(JSONObject textLineJson) {
        StringWriter writer = new StringWriter();
        try {
            JSONArray stringArray = textLineJson.getJSONArray("String");
            for (int i = 0; i < stringArray.length(); i++) {
                JSONObject stringJson = stringArray.getJSONObject(i);
                String value = processStringJSON(stringJson);
                if (value != null) {
                    writer.append(value).append(" ");
                }
            }
        } catch (JSONException ex) {
            if (ex.getMessage().contains("is not a JSONArray")) {
                JSONObject stringJson = textLineJson.getJSONObject("String");
                String value = processStringJSON(stringJson);
                if (value != null && !value.isBlank()) {
                    writer.append(value.trim()).append(" ");
                }
            } else {
                throw ex;
            }
        }
        return writer.toString().trim();
    }

    private static String processStringJSON(JSONObject stringJson) {
        String value = transformJsonValue(stringJson.get("CONTENT"));
        return (value != null && !value.isEmpty() && !value.isBlank()) ? value : null;
    }

    public static String transformJsonValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return String.valueOf(value);
        } else if (value instanceof BigDecimal) {
            return String.valueOf(value);
        } else {
            return value.toString();
        }
    }
}
