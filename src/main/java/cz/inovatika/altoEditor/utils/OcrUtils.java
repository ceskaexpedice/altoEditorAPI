package cz.inovatika.altoEditor.utils;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class OcrUtils {

    public static String createOcrFromAlto(String alto) {
        if (alto == null || alto.isBlank()) {
            return null;
        }

        JSONObject jsonObject = XML.toJSONObject(alto);

        JSONObject printSpace = extractPrintSpace(jsonObject);

        if (printSpace == null) {
            return null;
        }

        String result = processPrintSpace(printSpace);
        if (result == null) {
            return null;
        }

        return result.replaceAll("\n{2,}", "\n").trim();
    }

    private static JSONObject extractPrintSpace(JSONObject root) {
        if (root == null) {
            return null;
        }

        JSONObject alto = root.optJSONObject("alto");
        if (alto == null) {
            return null;
        }

        JSONObject layout = alto.optJSONObject("Layout");
        if (layout == null) {
            return null;
        }

        JSONObject page = layout.optJSONObject("Page");
        if (page == null) {
            return null;
        }

        return page.optJSONObject("PrintSpace");
    }

    private static String processPrintSpace(JSONObject printSpace) {
        StringBuilder sb = new StringBuilder();

        for (JSONObject textBlock : getObjects(printSpace, "TextBlock")) {
            String value = processTextBlock(textBlock);
            if (value != null && !value.isBlank()) {
                sb.append(value).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    private static String processTextBlock(JSONObject textBlock) {
        StringBuilder sb = new StringBuilder();

        for (JSONObject textLine : getObjects(textBlock, "TextLine")) {
            String value = processTextLine(textLine);
            if (value != null && !value.isBlank()) {
                sb.append(value).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private static String processTextLine(JSONObject textLine) {
        StringBuilder sb = new StringBuilder();

        for (JSONObject stringObj : getObjects(textLine, "String")) {
            String value = processString(stringObj);
            if (value != null && !value.isBlank()) {
                sb.append(value).append(" ");
            }
        }

        return sb.toString().trim();
    }

    private static String processString(JSONObject stringJson) {
        if (stringJson == null) {
            return null;
        }

        Object content = stringJson.opt("CONTENT");
        String value = transformJsonValue(content);

        return (value == null || value.isBlank()) ? null : value;
    }

    public static String transformJsonValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static List<JSONObject> getObjects(JSONObject parent, String key) {
        List<JSONObject> result = new ArrayList<>();

        if (parent == null || key == null) {
            return result;
        }

        Object obj = parent.opt(key);

        if (obj instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    result.add(item);
                }
            }
        } else if (obj instanceof JSONObject single) {
            result.add(single);
        }

        return result;
    }
}
