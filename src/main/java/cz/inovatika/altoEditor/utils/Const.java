package cz.inovatika.altoEditor.utils;

import java.util.HashMap;
import java.util.Map;

import static cz.inovatika.altoEditor.utils.Utils.getPath;

public class Const {

    public static final String APP_HOME = ".altoEditor";
    public static final String CONFIG_FILE_NAME = "application.conf";

    public static final String DEFAULT_RESOURCE_CONFIG = "cz/inovatika/altoEditor/server/application.conf";
    public static final String DEFAULT_RESOURCE_SQL = "cz/inovatika/altoEditor/db/dbInit.sql";

    public static final String USER_ALTOEDITOR = "AltoEditor";
    public static final String USER_PERO = "PERO";

    public static final String RESPONSE_CONTENT_TYPE = "text/html; charset=utf-8";

    public static final String DIGITAL_OBJECT_STATE_NEW = "NEW";
    public static final String DIGITAL_OBJECT_STATE_EDITED = "EDITED";
    public static final String DIGITAL_OBJECT_STATE_ACCEPTED = "ACCEPTED";
    public static final String DIGITAL_OBJECT_STATE_REJECTED = "REJECTED";
    public static final String DIGITAL_OBJECT_STATE_UPLOADED = "UPLOADED";
    public static final String DIGITAL_OBJECT_STATE_GENERATED = "GENERATED";

    public static final String BATCH_STATE_PLANNED = "PLANNED";
    public static final String BATCH_STATE_RUNNING = "RUNNING";
    public static final String BATCH_STATE_DONE = "DONE";
    public static final String BATCH_STATE_FAILED = "FAILED";

    public static final String BATCH_SUBSTATE_DOWNLOADING = "DOWNLOADING";
    public static final String BATCH_SUBSTATE_GENERATING = "GENERATING";
    public static final String BATCH_SUBSTATE_SAVING = "SAVING";

    public static final String BATCH_PRIORITY_LOW = "LOW";
    public static final String BATCH_PRIORITY_MEDIUM = "MEDIUM";
    public static final String BATCH_PRIORITY_HIGH = "HIGH";

    public static final String BATCH_TYPE_MULTIPLE = "MULTIPLE";
    public static final String BATCH_TYPE_SINGLE = "SINGLE";

    public static final String FOXML_PROPERTY_CREATEDATE = "info:fedora/fedora-system:def/model#createdDate";
    public static final String FOXML_PROPERTY_LABEL = "info:fedora/fedora-system:def/model#label";
    public static final String FOXML_PROPERTY_LASTMODIFIED = "info:fedora/fedora-system:def/view#lastModifiedDate";
    public static final String FOXML_PROPERTY_OWNER = "info:fedora/fedora-system:def/model#ownerId";
    public static final String FOXML_PROPERTY_STATE = "info:fedora/fedora-system:def/model#state";
    public static final String FOXML_PROPERTY_STATE_ACTIVE = "Active";
    public static final String FOXML_PROPERTY_STATE_DEACTIVE = "Deactive";

    public static final String PATH_ROOT = "/";

    public static final String PATH_APP = getPath(PATH_ROOT, "altoEditor");

    public static final String PATH_INFO = getPath(PATH_APP, "info");

    public static final String PATH_DB = getPath(PATH_APP, "db");
    public static final String PATH_DB_VERSIONS = getPath(PATH_DB, "versions");
    public static final String PATH_DB_ACTUAL_VERSION = getPath(PATH_DB, "actualVersion");
    public static final String PATH_DB_USERS = getPath(PATH_DB, "users");
    public static final String PATH_DB_USER = getPath(PATH_DB, "user");
    public static final String PATH_DB_DIGITAL_OBJECTS = getPath(PATH_DB, "objects");
    public static final String PATH_DB_DIGITAL_OBJECT = getPath(PATH_DB, "object");
    public static final String PATH_DB_BATCHES = getPath(PATH_DB, "batches");
    public static final String PATH_DB_BATCH = getPath(PATH_DB, "batch");

    public static final String PATH_DIGITAL_OBJECT = getPath(PATH_APP, "object");
    public static final String PATH_DIGITAL_OBJECT_IMAGE = getPath(PATH_DIGITAL_OBJECT, "image");
    public static final String PATH_DIGITAL_OBJECT_ALTO = getPath(PATH_DIGITAL_OBJECT, "alto");
    public static final String PATH_DIGITAL_OBJECT_PERO_GENERATE = getPath(PATH_DIGITAL_OBJECT, "pero");
    public static final String PATH_DIGITAL_OBJECT_STATE_ACCEPTED = getPath(PATH_DIGITAL_OBJECT, "stateAccepted");
    public static final String PATH_DIGITAL_OBJECT_STATE_REJECTED = getPath(PATH_DIGITAL_OBJECT, "stateRejected");
    public static final String PATH_DIGITAL_OBJECT_UPLOAD_KRAMERIUS = getPath(PATH_DIGITAL_OBJECT, "uploadKramerius");

    public static Map<String, String> MIMETYPE_MAP = new HashMap<>();

    static {
        MIMETYPE_MAP.put("ALTO", "application/xml");
        MIMETYPE_MAP.put("OCR", "text/plain");
    }
}
