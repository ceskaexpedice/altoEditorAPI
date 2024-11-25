package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import cz.inovatika.altoEditor.models.ApplicationVersion;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.utils.Utils.setContext;
import static cz.inovatika.altoEditor.utils.Utils.setResult;

public class InfoResource {

    private static final Logger LOGGER = LogManager.getLogger(InfoResource.class.getName());

    public static void info(Context ctx) {
        if (401 == ctx.res().getStatus() || 403 == ctx.res().getStatus()) {
            setResult(ctx, AltoEditorResponse.asError(ctx.res().getStatus(), ctx.result()));
            return;
        }
        setContext(ctx, "application/json; charset=utf-8");
        setResult(ctx, new AltoEditorResponse(new ApplicationVersion()));
    }
}
