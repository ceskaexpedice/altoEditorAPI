package cz.inovatika.altoEditor.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.altoEditor.db.DataSource;
import cz.inovatika.altoEditor.process.FileGeneratorProcess;
import cz.inovatika.altoEditor.process.ProcessDispatcher;
import cz.inovatika.altoEditor.resource.DbResource;
import cz.inovatika.altoEditor.resource.DigitalObjectResource;
import cz.inovatika.altoEditor.resource.InfoResource;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Configurator;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.user.UserProfile;
import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.user.UserUtils.getToken;
import static cz.inovatika.altoEditor.user.UserUtils.getUserProfile;
import static cz.inovatika.altoEditor.user.UserUtils.isAdminRolePath;
import static cz.inovatika.altoEditor.user.UserUtils.isAltoEditorRolePath;
import static cz.inovatika.altoEditor.user.UserUtils.isPublicPath;
import static cz.inovatika.altoEditor.utils.Const.CONFIG_FILE_NAME;
import static cz.inovatika.altoEditor.utils.Const.DEFAULT_RESOURCE_CONFIG;
import static cz.inovatika.altoEditor.utils.Utils.checkFile;
import static cz.inovatika.altoEditor.utils.Utils.getDefaultHome;
import static cz.inovatika.altoEditor.utils.Utils.readFile;

public class AltoEditorInitializer {

    private static final Logger LOGGER = LogManager.getLogger(AltoEditorInitializer.class.getName());
    public static final ObjectMapper mapper = new ObjectMapper();

    public void start() {
        try {
            initHome();
            initStorage();
            initDb();
            initProcesses();
            initApi();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void initProcesses() {
        try {
            FileGeneratorProcess.stopRunningBatches();
        } catch (Throwable t) {
            LOGGER.error("Impossible to stop running batches");
        }

        ProcessDispatcher dispatcher = new ProcessDispatcher();
        ProcessDispatcher.setDefault(dispatcher);
        dispatcher.init();
//        try {
//            FileGeneratorProcess.resumeAll(dispatcher);
//        } catch (Throwable t) {
//            LOGGER.error("Impossible to resume planned batches.");
//        }
    }

    private static void initHome() throws IOException {
        File appHome = getDefaultHome();
        if (!appHome.exists()) {
            appHome.mkdirs();
            LOGGER.info("Created default home folder: {}", appHome.getAbsolutePath());
        }
        checkFile(appHome, true, true, true, true);
        copyDefaultConfig(appHome);
    }

    private void initStorage() throws IOException {
        File objectStore = new File(Config.getObjectStorePath());
        if (!objectStore.exists()) {
            objectStore.mkdirs();
        }

        File dataStreamStore = new File(Config.getDataStreamStorePath());
        if (!dataStreamStore.exists()) {
            dataStreamStore.mkdirs();
        }
        LOGGER.info("Akubra storage set: " +
                "ObjectStore (" + objectStore.getAbsolutePath() + ") exists: " + objectStore.exists() + " pattern: " + Config.getObjectStorePattern() + ". " +
                "DataStreamStore (" + dataStreamStore.getAbsolutePath() + ") exists: " + dataStreamStore.exists() + " pattern: " + Config.getDataStreamStorePattern()+ ".");

        // test if storage is ok
        AkubraStorage storage = AkubraStorage.getInstance();
    }

    private static void copyDefaultConfig(File appHome) throws IOException {
        File cfgFile = new File(appHome, CONFIG_FILE_NAME);
        if (!cfgFile.exists()) {
            InputStream resource = readFile(DEFAULT_RESOURCE_CONFIG);
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8.name()));
            try {
                PrintWriter writer = new PrintWriter(cfgFile, StandardCharsets.UTF_8.name());
                try {
                    for (String line; (line = reader.readLine()) != null; ) {
                        writer.println(line);
                    }
                    writer.println();
                } finally {
                    writer.close();
                }
            } finally {
                reader.close();
            }
        }
        Configurator.setAditionalConfigFile(cfgFile);
        LOGGER.info("Config is in {}", cfgFile.getAbsolutePath());
    }

    private void initApi() {
        Javalin app = Javalin.create().start(Config.getPort());

        // Middleware pro autentizaci na chráněných endpointech
        app.before(ctx -> {
            if (isPublicPath(ctx.path())) {
                // pokracuje na public ednpointy
            } else {
                String token = getToken(ctx);
                if (token == null) {
                    ctx.status(401).result("Unauthorized");
                    LOGGER.warn("Unauthorized access (token is null) to endpoint: " + ctx.path());
                } else {
                    UserProfile user = getUserProfile(token);
                    if (user == null || user.getRoles().isEmpty()) {
                        ctx.status(401).result("Unauthorized");
                        LOGGER.warn("Unauthorized access (user roles is null) to endpoint: " + ctx.path());
                    } else {
                        if (isAdminRolePath(ctx.path(), user)) {
                            // pokracuje jako admin user k endpointum api
                        } else if (isAltoEditorRolePath(ctx.path(), user)) {
                            // pokracuje jako alto editor k endpointum api
                        } else {
                            ctx.status(403).result("Forbidden");
                            LOGGER.warn("Forbidden access (user = " + user.getUsername() + ") to endpoint: " + ctx.path());
                        }
                    }
                }
            }

        });
        app.before(ctx -> ctx.res().setCharacterEncoding(StandardCharsets.UTF_8.name()));
        app.before(ctx -> ctx.res().setContentType("application/json; charset=utf-8"));

        app.get(Const.PATH_ROOT, InfoResource::info);
        app.get(Const.PATH_APP, InfoResource::info);
        app.get(Const.PATH_INFO, InfoResource::info);
        app.get(Const.PATH_DB, DbResource::showSchema);
        app.post(Const.PATH_DB, DbResource::createSchema);
        app.get(Const.PATH_DB_VERSIONS, DbResource::getVersions);
        app.get(Const.PATH_DB_ACTUAL_VERSION, DbResource::getVersion);
        app.get(Const.PATH_DB_USERS, DbResource::getAllUsers);
        app.get(Const.PATH_DB_USER, DbResource::getUser);
        app.post(Const.PATH_DB_USER, DbResource::createUser);
//        app.put(Const.PATH_DB_USER, DbResource::updateUser);
        app.get(Const.PATH_DB_DIGITAL_OBJECTS, DbResource::getAllDigitalObjects);
        app.get(Const.PATH_DB_DIGITAL_OBJECT, DbResource::getUsersDigitalObjects);
        app.post(Const.PATH_DB_DIGITAL_OBJECT, DbResource::createDigitalObject);
        app.put(Const.PATH_DB_DIGITAL_OBJECT, DbResource::updateDigitalObject);
        app.get(Const.PATH_DB_BATCHES, DbResource::getBatches);
        app.get(Const.PATH_DB_BATCH, DbResource::getBatches);
        app.get(Const.PATH_DIGITAL_OBJECT_INFORMATION, DigitalObjectResource::getObjectInformation);
        app.get(Const.PATH_DIGITAL_OBJECT_IMAGE, DigitalObjectResource::getImage);
        app.get(Const.PATH_DIGITAL_OBJECT_ALTO, DigitalObjectResource::getAlto);
        app.get(Const.PATH_DIGITAL_OBJECT_ALTO_ORIGINAL, DigitalObjectResource::getAltoOriginal);
        app.get(Const.PATH_DIGITAL_OBJECT_OCR, DigitalObjectResource::getOcr);
        app.post(Const.PATH_DIGITAL_OBJECT_ALTO, DigitalObjectResource::updateAlto);
        app.post(Const.PATH_DIGITAL_OBJECT_PERO_GENERATE, DigitalObjectResource::generatePero);
        app.post(Const.PATH_DIGITAL_OBJECT_STATE_ACCEPTED, DigitalObjectResource::stateAccepted);
        app.post(Const.PATH_DIGITAL_OBJECT_STATE_REJECTED, DigitalObjectResource::stateRejected);
        app.post(Const.PATH_DIGITAL_OBJECT_UPLOAD_KRAMERIUS, DigitalObjectResource::uploadKramerius);
        app.post(Const.PATH_DIGITAL_OBJECT_LOCK, DigitalObjectResource::lockDigitalObject);
        app.post(Const.PATH_DIGITAL_OBJECT_UNLOCK, DigitalObjectResource::unlockDigitalObject);
    }

    private static void initDb() {
        String url = Config.getJdbcUrl();
        String driver = Config.getJdbcDriver();
        String password = Config.getJdbcPassword();
        String username = Config.getJdbcUserName();
        int poolSize = Config.getJdbcPoolSize();

        DataSource.configure(url, driver, username, password, poolSize);
        LOGGER.info("Connection to DB established.");
    }
}
