package cz.inovatika.altoEditor.resource;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.filter.BatchFilter;
import cz.inovatika.altoEditor.db.filter.UserFilter;
import cz.inovatika.altoEditor.db.manager.BatchManager;
import cz.inovatika.altoEditor.db.manager.UserManager;
import cz.inovatika.altoEditor.db.model.Batch;
import cz.inovatika.altoEditor.db.model.User;
import cz.inovatika.altoEditor.db.model.Version;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Authentication;

import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_FORBIDDEN;
import static cz.inovatika.altoEditor.response.AltoEditorResponse.RESPONSE_UNAUTHORIZED;
import static cz.inovatika.altoEditor.user.UserUtils.getToken;
import static cz.inovatika.altoEditor.user.UserUtils.getUserProfile;
import static cz.inovatika.altoEditor.utils.Const.DEFAULT_RESOURCE_SQL;
import static cz.inovatika.altoEditor.utils.Utils.getBooleanNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getIntegerNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptIntegerRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptTimeStampRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.readFile;
import static cz.inovatika.altoEditor.utils.Utils.setContext;
import static cz.inovatika.altoEditor.utils.Utils.setResult;

public class DbResource {

    private static final Logger LOGGER = LogManager.getLogger(DbResource.class.getName());

    public static void showSchema(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            InputStream resource = readFile(DEFAULT_RESOURCE_SQL);
            setContext(context, null);
            context.result(resource);
        } catch (IOException ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    // TODO
    public static void createSchema(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            boolean update = getBooleanNodeValue(node, "update");
            if (update) {
                setResult(context, new AltoEditorResponse("SQL script executed successfully."));
            } else {
                showSchema(context);
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getVersions(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            List<Version> versionList = Manager.getAllVersions();
            setResult(context, new AltoEditorResponse(versionList));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getVersion(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            Version version = Manager.getActualVersion();
            setResult(context, new AltoEditorResponse(version));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllUsers(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            UserManager userManager = UserManager.getInstance();
            UserFilter filter = UserFilter.builder().build();
            List<User> users = userManager.findUser(filter);
            setResult(context, new AltoEditorResponse(users));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getUser(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        try {
            String login = getOptStringRequestValue(context, Const.PARAM_USER_LOGIN);

            UserManager userManager = UserManager.getInstance();
            UserFilter filter = UserFilter.builder().login(login).build();
            List<User> users = userManager.findUser(filter);
            setResult(context, new AltoEditorResponse(users));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }

    }

    public static void createUser(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            UserManager userManager = UserManager.getInstance();
            UserFilter filter = UserFilter.builder().login(userProfile.getUsername()).build();
            List<User> users = userManager.findUser(filter);
            if (users != null && !users.isEmpty()) {
                throw new IOException(String.format("User login \"%s\" already exists.", users.get(0).getLogin()));
            } else {
                User user = userManager.addNewUser(userProfile.getUsername());
                setResult(context, new AltoEditorResponse(user));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void updateUser(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }

        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            Integer userId = getIntegerNodeValue(node, Const.PARAM_USER_USERID);
            String login = getStringNodeValue(node, Const.PARAM_USER_LOGIN);

            UserManager userManager = UserManager.getInstance();

            UserFilter filter = UserFilter.builder().login(login).build();
            List<User> users = userManager.findUser(filter);
            if (users != null && !users.isEmpty()) {
                throw new IOException(String.format("User with this login \"%s\" already exists.", login));
            }

            filter = UserFilter.builder().id(userId).build();
            users = userManager.findUser(filter);
            if (users == null || users.isEmpty()) {
                throw new IOException(String.format("User with id \"%s\" does not exists.", userId));
            } else if (users.size() > 1) {
                throw new IOException(String.format("More than one User with id \"%s\".", userId));
            } else {
                User user = users.get(0);
                user.setLogin(login);

                user = userManager.updateUser(user);
                setResult(context, new AltoEditorResponse(user));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllBatches(Context context) {
        try {
            String orderBy = getOptStringRequestValue(context, Const.PARAM_ORDER_BY);
            if (orderBy != null) {
                if (!(Const.PARAM_BATCH_ID.equals(orderBy) || Const.PARAM_BATCH_PID.equals(orderBy) || Const.PARAM_BATCH_CREATE_DATE.equals(orderBy)
                        || Const.PARAM_BATCH_UPDATE_DATE.equals(orderBy) || Const.PARAM_BATCH_STATE.equals(orderBy) || Const.PARAM_BATCH_SUBSTATE.equals(orderBy)
                        || Const.PARAM_BATCH_PRIORITY.equals(orderBy) || Const.PARAM_BATCH_TYPE.equals(orderBy) || Const.PARAM_BATCH_INSTANCE.equals(orderBy)
                        || Const.PARAM_BATCH_ESTIMATE_ITEM_NUMBER.equals(orderBy) || Const.PARAM_BATCH_LOG.equals(orderBy))) {
                    throw new RequestException(Const.PARAM_ORDER_BY, String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, Const.PARAM_ORDER_SORT);
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException(Const.PARAM_ORDER_SORT, String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            BatchManager batchManager = BatchManager.getInstance();
            BatchFilter filter = BatchFilter.builder().orderBy(orderBy).orderSort(orderSort).build();
            List<Batch> batches = batchManager.findBatch(filter);
            setResult(context, new AltoEditorResponse(batches));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getBatches(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
//            String login = getOptStringRequestValue(context, "login");
            Integer id = getOptIntegerRequestValue(context, Const.PARAM_BATCH_ID);
            String pid = getOptStringRequestValue(context, Const.PARAM_BATCH_PID);
            Timestamp createDateFrom = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_CREATE_DATE_FROM);
            Timestamp createDateTo = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_CREATE_DATE_TO);
            Timestamp createDate = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_CREATE_DATE);
            Timestamp updateDateFrom = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_UPDATE_DATE_FROM);
            Timestamp updateDateTo = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_UPDATE_DATE_TO);
            Timestamp updateDate = getOptTimeStampRequestValue(context, Const.PARAM_BATCH_UPDATE_DATE);
            String state = getOptStringRequestValue(context, Const.PARAM_BATCH_STATE);
            String substate = getOptStringRequestValue(context, Const.PARAM_BATCH_SUBSTATE);
            String priority = getOptStringRequestValue(context, Const.PARAM_BATCH_PRIORITY);
            String type = getOptStringRequestValue(context, Const.PARAM_BATCH_TYPE);
            String instanceId = getOptStringRequestValue(context, Const.PARAM_BATCH_INSTANCE);
            Integer estimateItemNumber = getOptIntegerRequestValue(context, Const.PARAM_BATCH_ESTIMATE_ITEM_NUMBER);
            String log = getOptStringRequestValue(context, Const.PARAM_BATCH_LOG);

            String orderBy = getOptStringRequestValue(context, Const.PARAM_ORDER_BY);
            if (orderBy != null) {
                if (!(Const.PARAM_BATCH_ID.equals(orderBy) || Const.PARAM_BATCH_PID.equals(orderBy) || Const.PARAM_BATCH_CREATE_DATE.equals(orderBy)
                        || Const.PARAM_BATCH_UPDATE_DATE.equals(orderBy) || Const.PARAM_BATCH_STATE.equals(orderBy) || Const.PARAM_BATCH_SUBSTATE.equals(orderBy)
                        || Const.PARAM_BATCH_PRIORITY.equals(orderBy) || Const.PARAM_BATCH_TYPE.equals(orderBy) || Const.PARAM_BATCH_INSTANCE.equals(orderBy)
                        || Const.PARAM_BATCH_ESTIMATE_ITEM_NUMBER.equals(orderBy) || Const.PARAM_BATCH_LOG.equals(orderBy))) {
                    throw new RequestException(Const.PARAM_ORDER_BY, String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, Const.PARAM_ORDER_SORT);
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException(Const.PARAM_ORDER_SORT, String.format("Unsupported param \"%s\".", orderSort));
                }
            }

            Integer limit = getOptIntegerRequestValue(context, Const.PARAM_LIMIT);
            if (limit == null || limit < 0) {
                limit = Const.DEFAULT_SQL_LIMIT_SIZE;
            }
            Integer offset = getOptIntegerRequestValue(context, Const.PARAM_OFFSET);
            if (offset == null || offset < 0) {
                offset = 0;
            }

            BatchManager batchManager = BatchManager.getInstance();
            BatchFilter filter = BatchFilter.builder()
                    .id(id).pid(pid)
                    .createDateFrom(createDateFrom).createDateTo(createDateTo).createDate(createDate)
                    .updateDateFrom(updateDateFrom).updateDateTo(updateDateTo).updateDate(updateDate)
                    .state(state).substate(substate).priority(priority).type(type).instance(instanceId).estimateItemNumber(estimateItemNumber).log(log)
                    .orderBy(orderBy).orderSort(orderSort).limit(limit).offset(offset)
                    .build();
            int totalCount = batchManager.getBatchesCount(filter);
            List<Batch> batches = batchManager.findBatch(filter);
            setResult(context, new AltoEditorResponse(batches, offset, totalCount));

        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    private static void checkDateFormat(String key, String date) throws RequestException {
        if (date == null) {
            return;
        } else {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            formatter.setLenient(false);
            try {
                formatter.parse(date);
            } catch (ParseException e) {
                throw new RequestException(key, String.format("Unsupported format \"%s\".", date));
            }
        }
    }

    public static void getAllDigitalObjects(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            String id = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_ID);
            String rUserId = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_RUSERID);
            String instance = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);
            String pid = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            String versionXml = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_VERSION_XML);
            String datum = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_DATUM);
            String state = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_STATE);
            String label = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_LABEL);
            String parentLabel = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PARENT_LABEL);
            String parentPath = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PARENT_PATH);
            String login = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_USER_LOGIN);

            String orderBy = getOptStringRequestValue(context, Const.PARAM_ORDER_BY);
            if (orderBy != null) {
                if (!(Const.PARAM_DIGITAL_OBJECT_ID.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_RUSERID.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_INSTANCE.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_PID.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_VERSION_XML.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_DATUM.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_STATE.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_LABEL.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_PARENT_LABEL.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_PARENT_PATH.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_USER_LOGIN.equals(orderBy))) {
                    throw new RequestException(Const.PARAM_ORDER_BY, String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, Const.PARAM_ORDER_SORT);
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException(Const.PARAM_ORDER_SORT, String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            Integer limit = getOptIntegerRequestValue(context, Const.PARAM_LIMIT);
            if (limit == null || limit < 0) {
                limit = Const.DEFAULT_SQL_LIMIT_SIZE;
            }
            Integer offset = getOptIntegerRequestValue(context, Const.PARAM_OFFSET);
            if (offset == null || offset < 0) {
                offset = 0;
            }
            int totalCount = Manager.getDigitalObjectsCount(id, rUserId, instance, pid, versionXml, datum, state, label, parentLabel, parentPath, login);
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(id, rUserId, instance, pid, versionXml,
                    datum, state, label, parentLabel, parentPath, login, orderBy, orderSort, limit, offset);
            setResult(context, new AltoEditorResponse(digitalObjects, offset, totalCount));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getUsersDigitalObjects(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            String id = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_ID);
            String rUserId = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_RUSERID);
            String instance = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_INSTANCE);
            String pid = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PID);
            String versionXml = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_VERSION_XML);
            String datum = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_DATUM);
            String state = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_STATE);
            String label = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_LABEL);
            String parentLabel = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PARENT_LABEL);
            String parentPath = getOptStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_PARENT_PATH);
//            String login = getStringRequestValue(context, Const.PARAM_DIGITAL_OBJECT_USER_LOGIN);

            String orderBy = getOptStringRequestValue(context, Const.PARAM_ORDER_BY);
            if (orderBy != null) {
                if (!(Const.PARAM_DIGITAL_OBJECT_ID.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_RUSERID.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_INSTANCE.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_PID.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_VERSION_XML.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_DATUM.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_STATE.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_LABEL.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_PARENT_LABEL.equals(orderBy) || Const.PARAM_DIGITAL_OBJECT_PARENT_PATH.equals(orderBy)
                        || Const.PARAM_DIGITAL_OBJECT_USER_LOGIN.equals(orderBy))) {
                    throw new RequestException(Const.PARAM_ORDER_BY, String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, Const.PARAM_ORDER_SORT);
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException(Const.PARAM_ORDER_SORT, String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            Integer limit = getOptIntegerRequestValue(context, Const.PARAM_LIMIT);
            if (limit == null || limit < 0) {
                limit = Const.DEFAULT_SQL_LIMIT_SIZE;
            }
            Integer offset = getOptIntegerRequestValue(context, Const.PARAM_OFFSET);
            if (offset == null || offset < 0) {
                offset = 0;
            }
            int totalCount = Manager.getDigitalObjectsCount(id, rUserId, instance, pid, versionXml, datum, state, label, parentLabel, parentPath, userProfile.getUsername());
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(id, rUserId, instance, pid, versionXml,
                    datum, state, label, parentLabel, parentPath, userProfile.getUsername(), orderBy, orderSort, limit, offset);
            setResult(context, new AltoEditorResponse(digitalObjects, offset, totalCount));

        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void createDigitalObject(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String version = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_VERSION_XML);
            String instance = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_INSTANCE);

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(userProfile.getUsername(), pid);
            if (digitalObjects != null && !digitalObjects.isEmpty()) {
                throw new IOException(String.format("User login \"%s\" already exists.", userProfile.getUsername()));
            } else {

                Manager.createDigitalObject(userProfile, pid, version, instance);
                digitalObjects = Manager.getDigitalObjects(userProfile.getUsername(), pid);
                if (digitalObjects == null && digitalObjects.isEmpty()) {
                    throw new IOException(String.format("Digital object login \"%s\" and \"%s\" doees not exists.", userProfile.getUsername(), pid));
                } else if (digitalObjects.size() > 1) {
                    throw new IOException(String.format("There are more than 1 record with login \"%s\" and \"%s\" doees not exists.", userProfile.getUsername(), pid));
                } else {
                    setResult(context, new AltoEditorResponse(digitalObjects.get(0)));
                }
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void updateDigitalObject(Context context) {
        if (RESPONSE_UNAUTHORIZED == context.res().getStatus() || RESPONSE_FORBIDDEN == context.res().getStatus()) {
            setResult(context, AltoEditorResponse.asError(context.res().getStatus(), context.result()));
            return;
        }
        UserProfile userProfile = getUserProfile(getToken(context));
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String pid = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_PID);
            String version = getStringNodeValue(node, Const.PARAM_DIGITAL_OBJECT_VERSION_XML);

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(userProfile.getUsername(), pid);
            if (digitalObjects == null && digitalObjects.isEmpty()) {
                throw new IOException(String.format("Digital object login \"%s\" and \"%s\" does not exists.", userProfile.getUsername(), pid));
            } else if (digitalObjects.size() > 1) {
                throw new IOException(String.format("There are more than 1 record with login \"%s\" and \"%s\" doees not exists.", userProfile.getUsername(), pid));
            } else {
                DigitalObjectView digitalObject = digitalObjects.get(0);
                Manager.updateDigitalObject(digitalObject.getId(), version);
                digitalObject = Manager.getDigitalObjectById(digitalObject.getId());
                setResult(context, new AltoEditorResponse(digitalObject));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }
}
