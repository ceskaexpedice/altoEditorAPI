package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Manager;
import cz.inovatika.altoEditor.db.dao.Dao;
import cz.inovatika.altoEditor.db.dao.VersionDao;
import cz.inovatika.altoEditor.db.models.Batch;
import cz.inovatika.altoEditor.db.models.User;
import cz.inovatika.altoEditor.db.models.Version;
import cz.inovatika.altoEditor.exception.RequestException;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Const.DEFAULT_RESOURCE_SQL;
import static cz.inovatika.altoEditor.utils.Utils.getBooleanNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getOptStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.readFile;
import static cz.inovatika.altoEditor.utils.Utils.setContext;
import static cz.inovatika.altoEditor.utils.Utils.setResult;

public class DbResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbResource.class.getName());

    public static void showSchema(Context context) {
        try {
            InputStream resource = readFile(DEFAULT_RESOURCE_SQL);
            setContext(context, null);
            context.result(resource);
        } catch (IOException ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void createSchema(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            boolean update = getBooleanNodeValue(node, "update");
            if (update) {
                Dao dbDao = new Dao();
                dbDao.createSchema();
                setResult(context, new AltoEditorResponse("SQL script executed successfully."));
            } else {
                showSchema(context);
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getVersions(Context context) {
        try {
            VersionDao versionDao = new VersionDao();
            List<Version> versionList = versionDao.getAllVersions();
            setResult(context, new AltoEditorResponse(versionList));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getVersion(Context context) {
        try {
            VersionDao versionDao = new VersionDao();
            Version version = versionDao.getActualVersion();
            setResult(context, new AltoEditorResponse(version));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllUsers(Context context) {
        try {
            List<User> users = Manager.getAllUsers();
            setResult(context, new AltoEditorResponse(users));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getUser(Context context) {
        try {
            String login = getStringRequestValue(context, "login");
            User user = Manager.getUserByLogin(login);
            setResult(context, new AltoEditorResponse(user));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void createUser(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");

            User user = Manager.getUserByLogin(login);
            if (user != null) {
                throw new IOException(String.format("User login \"%s\" already exists.", login));
            } else {
                Manager.createUser(login);
                user = Manager.getUserByLogin(login);
                setResult(context, new AltoEditorResponse(user));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void updateUser(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            String userId = getStringNodeValue(node, "userId");

            User user = Manager.getUserById(userId);
            if (user == null) {
                throw new IOException(String.format("User with id \"%s\" does not exists.", userId));
            } else {
                Manager.updateUser(userId, login);
                user = Manager.getUserById(userId);
                setResult(context, new AltoEditorResponse(user));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllBatches(Context context) {
        try {
            String orderBy = getOptStringRequestValue(context, "orderBy");
            if (orderBy != null) {
                if (!("id".equals(orderBy) || "folder".equals(orderBy) || "create".equals(orderBy) || "datum".equals(orderBy) || "state".equals(orderBy) || "estimateitemnumber".equals(orderBy) || "log".equals(orderBy) || "priority".equals(orderBy))) {
                    throw new RequestException("orderBy", String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, "orderSort");
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException("orderSort", String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            List<Batch> batches = Manager.getAllBatches(orderBy, orderSort);
            setResult(context, new AltoEditorResponse(batches));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getBatches(Context context) {
        try {
//            String login = getOptStringRequestValue(context, "login");
            String id = getOptStringRequestValue(context, "id");
            String pid = getOptStringRequestValue(context, "pid");
            String createDate = getOptStringRequestValue(context, "createDate");
            String updateDate = getOptStringRequestValue(context, "updateDate");
            String state = getOptStringRequestValue(context, "state");
            String substate = getOptStringRequestValue(context, "substate");
            String priority = getOptStringRequestValue(context, "priority");
            String type = getOptStringRequestValue(context, "type");
            String instanceId = getOptStringRequestValue(context, "instance");
            String estimateItemNumber = getOptStringRequestValue(context, "estimateItemNumber");
            String log = getOptStringRequestValue(context, "log");

            String orderBy = getOptStringRequestValue(context, "orderBy");
            if (orderBy != null) {
                if (!("id".equals(orderBy) || "folder".equals(orderBy) || "create".equals(orderBy) || "datum".equals(orderBy) || "state".equals(orderBy) || "estimateitemnumber".equals(orderBy) || "log".equals(orderBy) || "priority".equals(orderBy))) {
                    throw new RequestException("orderBy", String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, "orderSort");
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException("orderSort", String.format("Unsupported param \"%s\".", orderSort));
                }
            }

            if (createDate != null || updateDate != null) {
                checkDateFormat("createDate", createDate);
                checkDateFormat("updateDate", updateDate);
            }

            List<Batch> batches = Manager.getBatches(id, pid, createDate, updateDate, state, substate, priority, type, instanceId, estimateItemNumber, log, orderBy, orderSort);
            setResult(context, new AltoEditorResponse(batches));

        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    private static void checkDateFormat(String key, String date) throws RequestException {
        if (date == null) {
            return;
        } else {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            formatter .setLenient(false);
            try {
                formatter.parse(date);
            } catch (ParseException e) {
                throw new RequestException(key, String.format("Unsupported format \"%s\".", date));
            }
        }
    }

    public static void getAllDigitalObjects(Context context) {
        try {
            String orderBy = getOptStringRequestValue(context, "orderBy");
            if (orderBy != null) {
                if (!("id".equals(orderBy) || "rUserId".equals(orderBy) || "instance".equals(orderBy) || "pid".equals(orderBy) || "version".equals(orderBy) || "datum".equals(orderBy) || "state".equals(orderBy))) {
                    throw new RequestException("orderBy", String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, "orderSort");
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException("orderSort", String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            List<DigitalObjectView> digitalObjects = Manager.getAllDigitalObjects(orderBy, orderSort);
            setResult(context, new AltoEditorResponse(digitalObjects));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getDigitalObjects(Context context) {
        try {
            String login = getOptStringRequestValue(context, "login");
            String pid = getOptStringRequestValue(context, "pid");

            String orderBy = getOptStringRequestValue(context, "orderBy");
            if (orderBy != null) {
                if (!("id".equals(orderBy) || "rUserId".equals(orderBy) || "instance".equals(orderBy) || "pid".equals(orderBy) || "version".equals(orderBy) || "datum".equals(orderBy) || "state".equals(orderBy))) {
                    throw new RequestException("orderBy", String.format("Unsupported param \"%s\".", orderBy));
                }
            }
            String orderSort = getOptStringRequestValue(context, "orderSort");
            if (orderSort != null) {
                if (!("asc".equals(orderSort) || "desc".equals(orderSort))) {
                    throw new RequestException("orderSort", String.format("Unsupported param \"%s\".", orderSort));
                }
            }
            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(login, pid, orderBy, orderSort);
            setResult(context, new AltoEditorResponse(digitalObjects));

        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void createDigitalObject(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            String pid = getStringNodeValue(node, "pid");
            String version = getStringNodeValue(node, "version");
            String instance = getStringNodeValue(node, "instance");

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(login, pid);
            if (digitalObjects != null && !digitalObjects.isEmpty()) {
                throw new IOException(String.format("User login \"%s\" already exists.", login));
            } else {

                Manager.createDigitalObject(login, pid, version, instance);
                digitalObjects = Manager.getDigitalObjects(login, pid);
                if (digitalObjects == null && digitalObjects.isEmpty()) {
                    throw new IOException(String.format("Digital object login \"%s\" and \"%s\" doees not exists.", login, pid));
                } else if (digitalObjects.size() > 1) {
                    throw new IOException(String.format("There are more than 1 record with login \"%s\" and \"%s\" doees not exists.", login, pid));
                } else {
                    setResult(context, new AltoEditorResponse(digitalObjects.get(0)));
                }
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void updateDigitalObject(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");
            String pid = getStringNodeValue(node, "pid");
            String version = getStringNodeValue(node, "version");

            List<DigitalObjectView> digitalObjects = Manager.getDigitalObjects(login, pid);
            if (digitalObjects == null && digitalObjects.isEmpty()) {
                throw new IOException(String.format("Digital object login \"%s\" and \"%s\" doees not exists.", login, pid));
            } else if (digitalObjects.size() > 1) {
                throw new IOException(String.format("There are more than 1 record with login \"%s\" and \"%s\" doees not exists.", login, pid));
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
