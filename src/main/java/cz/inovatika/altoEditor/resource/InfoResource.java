package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import cz.inovatika.altoEditor.models.ApplicationVersion;
import cz.inovatika.altoEditor.response.AltoEditorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Utils.setContext;
import static cz.inovatika.altoEditor.utils.Utils.setResult;

public class InfoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoResource.class.getName());

    public static void info(Context ctx) {
        setContext(ctx, "application/json; charset=utf-8");
        setResult(ctx, new AltoEditorResponse(new ApplicationVersion()));
    }
}
