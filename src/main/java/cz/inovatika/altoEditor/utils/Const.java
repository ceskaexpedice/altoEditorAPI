package cz.inovatika.altoEditor.utils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.inovatika.altoEditor.utils.Utils.getPath;

public class Const {

    public static final String APP_HOME = ".altoEditor";
    public static final String CONFIG_FILE_NAME = "application.conf";

    public static final String DEFAULT_RESOURCE_CONFIG = "cz/inovatika/altoEditor/server/application.conf";
    public static final String DEFAULT_RESOURCE_SQL = "cz/inovatika/altoEditor/db/dbInit.sql";

    public static final String USER_ALTOEDITOR = "AltoEditor";
    public static final String USER_PERO = "PERO";

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
    public static final String PATH_DIGITAL_OBJECT_INFORMATION = getPath(PATH_DIGITAL_OBJECT, "objectInformation");
    public static final String PATH_DIGITAL_OBJECT_IMAGE = getPath(PATH_DIGITAL_OBJECT, "image");
    public static final String PATH_DIGITAL_OBJECT_ALTO = getPath(PATH_DIGITAL_OBJECT, "alto");
    public static final String PATH_DIGITAL_OBJECT_ALTO_ORIGINAL = getPath(PATH_DIGITAL_OBJECT, "altoOriginal");
    public static final String PATH_DIGITAL_OBJECT_OCR = getPath(PATH_DIGITAL_OBJECT, "ocr");
    public static final String PATH_DIGITAL_OBJECT_PERO_GENERATE = getPath(PATH_DIGITAL_OBJECT, "pero");
    public static final String PATH_DIGITAL_OBJECT_STATE_ACCEPTED = getPath(PATH_DIGITAL_OBJECT, "stateAccepted");
    public static final String PATH_DIGITAL_OBJECT_STATE_REJECTED = getPath(PATH_DIGITAL_OBJECT, "stateRejected");
    public static final String PATH_DIGITAL_OBJECT_UPLOAD_KRAMERIUS = getPath(PATH_DIGITAL_OBJECT, "uploadKramerius");
    public static final String PATH_DIGITAL_OBJECT_LOCK = getPath(PATH_DIGITAL_OBJECT, "lock");
    public static final String PATH_DIGITAL_OBJECT_UNLOCK = getPath(PATH_DIGITAL_OBJECT, "unlock");

    public static List<String> PUBLIC_PATH = Arrays.asList(PATH_ROOT, PATH_APP, PATH_INFO);
    public static List<String> ADMINS_PATH = Arrays.asList(PATH_ROOT, PATH_APP, PATH_INFO, PATH_DB, PATH_DB_VERSIONS, PATH_DB_ACTUAL_VERSION,
            PATH_DB_USERS, PATH_DB_USER, PATH_DB_DIGITAL_OBJECTS, PATH_DB_DIGITAL_OBJECT, PATH_DB_BATCHES, PATH_DB_BATCH,
            PATH_DIGITAL_OBJECT, PATH_DIGITAL_OBJECT_INFORMATION, PATH_DIGITAL_OBJECT_IMAGE, PATH_DIGITAL_OBJECT_ALTO,
            PATH_DIGITAL_OBJECT_ALTO_ORIGINAL, PATH_DIGITAL_OBJECT_OCR, PATH_DIGITAL_OBJECT_PERO_GENERATE, PATH_DIGITAL_OBJECT_STATE_ACCEPTED,
            PATH_DIGITAL_OBJECT_STATE_REJECTED, PATH_DIGITAL_OBJECT_UPLOAD_KRAMERIUS, PATH_DIGITAL_OBJECT_LOCK, PATH_DIGITAL_OBJECT_UNLOCK);
    public static List<String> EDITORS_PATH = Arrays.asList(PATH_ROOT, PATH_APP, PATH_INFO, PATH_DB_VERSIONS, PATH_DB_ACTUAL_VERSION,
            PATH_DB_USERS, PATH_DB_USER, PATH_DB_DIGITAL_OBJECTS, PATH_DB_DIGITAL_OBJECT, PATH_DB_BATCHES, PATH_DB_BATCH,
            PATH_DIGITAL_OBJECT, PATH_DIGITAL_OBJECT_INFORMATION, PATH_DIGITAL_OBJECT_IMAGE, PATH_DIGITAL_OBJECT_ALTO,
            PATH_DIGITAL_OBJECT_ALTO_ORIGINAL, PATH_DIGITAL_OBJECT_OCR, PATH_DIGITAL_OBJECT_PERO_GENERATE);

    public static final String PARAM_BATCH_ID = "id";
    public static final String PARAM_BATCH_PID = "pid";
    public static final String PARAM_BATCH_CREATE_DATE = "createDate";
    public static final String PARAM_BATCH_UPDATE_DATE = "updateDate";
    public static final String PARAM_BATCH_STATE = "state";
    public static final String PARAM_BATCH_SUBSTATE = "substate";
    public static final String PARAM_BATCH_PRIORITY = "priority";
    public static final String PARAM_BATCH_TYPE = "type";
    public static final String PARAM_BATCH_INSTANCE = "instance";
    public static final String PARAM_BATCH_OBJECT_ID = "objectId";
    public static final String PARAM_BATCH_ESTIMATE_ITEM_NUMBER = "estimateItemNumber";
    public static final String PARAM_BATCH_LOG = "log";

    public static final String PARAM_DIGITAL_OBJECT_ID = "id";
    public static final String PARAM_DIGITAL_OBJECT_RUSERID = "rUserId";
    public static final String PARAM_DIGITAL_OBJECT_USER_LOGIN = "userLogin";
    public static final String PARAM_DIGITAL_OBJECT_INSTANCE = "instance";
    public static final String PARAM_DIGITAL_OBJECT_PID = "pid";
    public static final String PARAM_DIGITAL_OBJECT_VERSION_XML = "versionXml";
    public static final String PARAM_DIGITAL_OBJECT_DATUM = "datum";
    public static final String PARAM_DIGITAL_OBJECT_STATE = "state";
    public static final String PARAM_DIGITAL_OBJECT_LABEL = "label";
    public static final String PARAM_DIGITAL_OBJECT_PARENT_PATH = "parentPath";
    public static final String PARAM_DIGITAL_OBJECT_PARENT_LABEL = "parentLabel";
    public static final String PARAM_DIGITAL_OBJECT_DATA = "data";

    public static final String PARAM_VERSION_ID = "id";
    public static final String PARAM_VERSION_APLICATION = "application";
    public static final String PARAM_VERSION_VERSION = "version";
    public static final String PARAM_VERSION_DATUM = "datum";

    public static final String PARAM_USER_ID = "id";
    public static final String PARAM_USER_LOGIN = "login";
    public static final String PARAM_USER_USERID = "userId";

    public static final String PARAM_ORDER_BY = "orderBy";
    public static final String PARAM_ORDER_SORT = "orderSort";
    public static final String PARAM_LIMIT = "limit";
    public static final String PARAM_OFFSET = "offset";

    public static final Integer DEFAULT_SQL_LIMIT_SIZE = 10;

    public static final String DATASTREAM_TYPE_ALTO = "ALTO";
    public static final String DATASTREAM_TYPE_OCR = "OCR";

    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_TEXT = "text/plain";

    public static Map<String, String> MIMETYPE_MAP = new HashMap<>();


    static {
        MIMETYPE_MAP.put(DATASTREAM_TYPE_ALTO, CONTENT_TYPE_XML);
        MIMETYPE_MAP.put(DATASTREAM_TYPE_OCR, CONTENT_TYPE_TEXT);
    }
}
