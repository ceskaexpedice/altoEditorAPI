package cz.inovatika.altoEditor.resource;

import io.javalin.http.Context;

import com.fasterxml.jackson.databind.JsonNode;
import cz.inovatika.altoEditor.db.Dao;
import cz.inovatika.altoEditor.db.DigitalObject;
import cz.inovatika.altoEditor.db.User;
import cz.inovatika.altoEditor.db.Version;
import cz.inovatika.altoEditor.response.AltoEditorResponse;
import cz.inovatika.altoEditor.server.AltoEditorInitializer;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.Const.DEFAULT_RESOURCE_SQL;
import static cz.inovatika.altoEditor.utils.Utils.getBooleanNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringNodeValue;
import static cz.inovatika.altoEditor.utils.Utils.getStringRequestValue;
import static cz.inovatika.altoEditor.utils.Utils.readFile;
import static cz.inovatika.altoEditor.utils.Utils.setContext;
import static cz.inovatika.altoEditor.utils.Utils.setResult;

public class DbResource {

    private static final Logger LOG = LoggerFactory.getLogger(DbResource.class.getName());

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
            Dao dbDao = new Dao();
            List<Version> versionList = dbDao.getAllVersions();
            setResult(context, new AltoEditorResponse(versionList));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getVersion(Context context) {
        try {
            Dao dbDao = new Dao();
            Version version = dbDao.getActualVersion();
            setResult(context, new AltoEditorResponse(version));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllUsers(Context context) {
        try {
            Dao dbDao = new Dao();
            List<User> users = dbDao.getAllUsers();
            setResult(context, new AltoEditorResponse(users));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getUser(Context context) {
        try {
            String login = getStringRequestValue(context, "login");
            Dao dbDao = new Dao();
            User user = dbDao.getUserByLogin(login);
            setResult(context, new AltoEditorResponse(user));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void createUser(Context context) {
        try {
            JsonNode node = AltoEditorInitializer.mapper.readTree(context.body());
            String login = getStringNodeValue(node, "login");

            Dao dbDao = new Dao();
            User user = dbDao.getUserByLogin(login);
            if (user != null) {
                throw new IOException(String.format("User login \"%s\" already exists.", login));
            } else {
                dbDao.createUser(login);
                user = dbDao.getUserByLogin(login);
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

            Dao dbDao = new Dao();
            User user = dbDao.getUserById(userId);
            if (user == null) {
                throw new IOException(String.format("User with id \"%s\" does not exists.", userId));
            } else {
                dbDao.updateUser(userId, login);
                user = dbDao.getUserById(userId);
                setResult(context, new AltoEditorResponse(user));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getAllDigitalObjects(Context context) {
        try {
            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getAllDigitalObjects();
            setResult(context, new AltoEditorResponse(digitalObjects));
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }

    public static void getDigitalObjects(Context context) {
        try {
            String login = getStringRequestValue(context, "login");
            String pid = getStringRequestValue(context, "pid");

            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(login, pid);
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

            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(login, pid);
            if (digitalObjects != null && !digitalObjects.isEmpty()) {
                throw new IOException(String.format("User login \"%s\" already exists.", login));
            } else {
                dbDao.createDigitalObject(login, pid, version, instance);
                digitalObjects = dbDao.getDigitalObjects(login, pid);
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

            Dao dbDao = new Dao();
            List<DigitalObject> digitalObjects = dbDao.getDigitalObjects(login, pid);
            if (digitalObjects == null && digitalObjects.isEmpty()) {
                throw new IOException(String.format("Digital object login \"%s\" and \"%s\" doees not exists.", login, pid));
            } else if (digitalObjects.size() > 1) {
                throw new IOException(String.format("There are more than 1 record with login \"%s\" and \"%s\" doees not exists.", login, pid));
            } else {
                DigitalObject digitalObject = digitalObjects.get(0);
                dbDao.updateDigitalObject(digitalObject.getId(), version);
                digitalObject = dbDao.getDigitalObjectById(digitalObject.getId());
                setResult(context, new AltoEditorResponse(digitalObject));
            }
        } catch (Exception ex) {
            setResult(context, AltoEditorResponse.asError(ex));
        }
    }
}
