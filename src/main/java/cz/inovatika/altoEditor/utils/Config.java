package cz.inovatika.altoEditor.utils;

import cz.inovatika.utils.configuration.Configurator;

import java.util.Arrays;
import java.util.List;

import static cz.inovatika.altoEditor.utils.Utils.normalizePath;

public class Config {

    private static final String PROP_APPLICATION_PORT = "application.port";
    private static final String PROP_APPLICATION_VERSION = "application.version";

    private static final String PROP_APPLICATION_JDBC_DRIVER = "application.jdbc.driver";
    private static final String PROP_APPLICATION_JDBC_URL = "application.jdbc.url";
    private static final String PROP_APPLICATION_JDBC_USERNAME = "application.jdbc.username";
    private static final String PROP_APPLICATION_JDBC_PASSWORD = "application.jdbc.password";
    private static final String PROP_APPLICATION_JDBC_POOLSIZE = "application.jdbc.poolSize";

    private static final String PROP_APPLICATION_OBJECT_STORE_PATTERN = "application.objectStore.pattern";
    private static final String PROP_APPLICATION_OBJECT_STORE_PATH = "application.objectStore.path";
    private static final String PROP_APPLICATION_DATA_STREAM_STORE_PATTERN = "application.dataStreamStore.pattern";
    private static final String PROP_APPLICATION_DATA_STREAM_STORE_PATH = "application.dataStreamStore.path";
    private static final String PROP_APPLICATION_PERO_PATH = "application.pero.path";

    private static final String PROP_KRAMERIUS_INSTANCES = "krameriusInstances";
    private static final String PREFIX_KRAMERIUS_INSTANCE = "krameriusInstance";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_TITLE = "title";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_VERSION = "version";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL = "url";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_LOGIN = "urlLogin";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_PARAMETRIZED_IMPORT_QUERY = "urlParametrizedImportQuery";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_STATE_QUERY = "urlStateQuery";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_DOWNLOAD_FOXML = "urlDownloadFoxml";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_MODEL_INFO = "urlModelInfo";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_URL_IMAGE = "urlImage";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_USERNAME = "username";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_PASSWORD = "passwd";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_CLIENT_ID = "clientId";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_CLIENT_SECRET="clientSecret";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_GRANT_TYPE = "grantType";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_TYPE = "type";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_EXPORT_FOXML_FOLDER = "exportFoxmlFolder";
    private static final String SUFFIX_KRAMERIUS_INSTANCE_KRAMERIUS_IMPORT_FOXML_FOLDER = "krameriusImportFoxmlFolder";

    private static final String PROP_PROCESSOR_PERO_EXEC = "processor.pero.exec";
    private static final String PROP_PROCESSOR_PERO_ARG = "processor.pero.arg";
    private static final String PROP_PROCESSOR_PERO_TIMEOUT = "processor.pero.timeout";
    private static final String PROP_PROCESSOR_PERO_KEY = "processor.pero.key";


    public static final String getVersion() {
        return Configurator.get().getConfig().getString(PROP_APPLICATION_VERSION);
    }

    public static final String getAppHome() {
        return Configurator.get().getConfig().getString(Configurator.HOME);
    }

    public static final String getJdbcDriver() {
        return Configurator.get().getConfig().getString(PROP_APPLICATION_JDBC_DRIVER);
    }

    public static final String getJdbcUrl() {
        return Configurator.get().getConfig().getString(PROP_APPLICATION_JDBC_URL);
    }

    public static final String getJdbcUserName() {
        return Configurator.get().getConfig().getString(PROP_APPLICATION_JDBC_USERNAME);
    }

    public static final String getJdbcPassword() {
        return Configurator.get().getConfig().getString(PROP_APPLICATION_JDBC_PASSWORD);
    }

    public static final Integer getJdbcPoolSize() {
        return Configurator.get().getConfig().getInt(PROP_APPLICATION_JDBC_POOLSIZE);
    }

    public static final Integer getPort() {
        return Configurator.get().getConfig().getInt(PROP_APPLICATION_PORT);
    }

    public static final String getObjectStorePattern() {
        String pattern = Configurator.get().getConfig().getString(PROP_APPLICATION_OBJECT_STORE_PATTERN);
        return pattern.replaceAll("x", "#");
    }

    public static final String getObjectStorePath() {
        return normalizePath(Configurator.get().getConfig().getString(PROP_APPLICATION_OBJECT_STORE_PATH));
    }

    public static final String getDataStreamStorePattern() {
        String pattern = Configurator.get().getConfig().getString(PROP_APPLICATION_DATA_STREAM_STORE_PATTERN);
        return pattern.replaceAll("x", "#");
    }

    public static final String getDataStreamStorePath() {
        return normalizePath(Configurator.get().getConfig().getString(PROP_APPLICATION_DATA_STREAM_STORE_PATH));
    }

    public static final String getPeroPath() {
        return normalizePath(Configurator.get().getConfig().getString(PROP_APPLICATION_PERO_PATH));
    }

    public static final List<String> getKrameriusInstances() {
        String instances = Configurator.get().getConfig().getString(PROP_KRAMERIUS_INSTANCES);
        return Arrays.asList(instances.split(","));
    }

    public static final String getKrameriusInstanceId(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance);
    }

    public static final String getKrameriusInstanceVersion(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_VERSION);
    }

    public static final String getKrameriusInstanceUrlLogin(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_LOGIN);
    }

    public static final String getKrameriusInstancePassword(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_PASSWORD);
    }

    public static final String getKrameriusInstanceClientId(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_CLIENT_ID);
    }

    public static final String getKrameriusInstanceClientSecret(String instance) {
       return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_CLIENT_SECRET);
    }

    public static final String getKrameriusInstanceGrantType(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_GRANT_TYPE);
    }

    public static final String getKrameriusInstanceTitle(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_TITLE);
    }

    public static final String getKrameriusInstanceUrl(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL);
    }

    public static final String getKrameriusInstanceUsername(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_USERNAME);
    }

    public static final String getKrameriusInstanceExportFoxmlFolder(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_EXPORT_FOXML_FOLDER);
    }

    public static final String getKrameriusInstanceType(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_TYPE);
    }

    public static final String getKrameriusInstanceUrlParametrizedImportQuery(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_PARAMETRIZED_IMPORT_QUERY);
    }

    public static final String getKrameriusInstanceUrlImage(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_IMAGE);
    }

    public static final String getKrameriusInstanceUrlStateQuery(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_STATE_QUERY);
    }

    public static final String getKrameriusInstanceUrlDownloadFoxml(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_DOWNLOAD_FOXML);
    }

    public static final String getKrameriusInstanceUrlModelInfo(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_URL_MODEL_INFO);
    }

    public static final String getKrameriusInstanceKrameriusImportFoxmlFolder(String instance) {
        return Configurator.get().getConfig().getString(PREFIX_KRAMERIUS_INSTANCE + "." + instance + "." + SUFFIX_KRAMERIUS_INSTANCE_KRAMERIUS_IMPORT_FOXML_FOLDER);
    }

    public static final String getProcessorPeroExec() {
        return Configurator.get().getConfig().getString(PROP_PROCESSOR_PERO_EXEC);
    }

    public static final String getProcessorPeroArg() {
        return Configurator.get().getConfig().getString(PROP_PROCESSOR_PERO_ARG);
    }

    public static final long getProcessorPeroTimeout() {
        return Configurator.get().getConfig().getLong(PROP_PROCESSOR_PERO_TIMEOUT);
    }

    public static final String getProcessorPeroKey() {
        return Configurator.get().getConfig().getString(PROP_PROCESSOR_PERO_KEY);
    }
}
