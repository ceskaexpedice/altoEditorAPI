package cz.inovatika.altoEditor.kramerius;

import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.FileUtils.deleteFolder;
import static cz.inovatika.altoEditor.utils.FileUtils.getFile;
import static cz.inovatika.altoEditor.utils.FileUtils.writeToFile;
import static cz.inovatika.altoEditor.utils.OcrUtils.createOcrFromAlto;

/**
 * Utility class providing various functions related to the management of
 * digital objects in a repository system, specifically those involving
 * FOXML and ALTO files.
 *
 * This class includes methods for downloading, processing, and importing
 * digital objects, handling ALTO OCR uploads, and managing temporary file
 * storage during operations.
 */
public class K7Utility {

    private static final Logger LOGGER = LogManager.getLogger(K7Utility.class.getName());

    public void downloadFoxml(String pid, String model, K7Client client, UserProfile userProfile) throws AltoEditorException, IOException {
        AkubraStorage storage = AkubraStorage.getInstance();
        if (storage.exist(pid)) {
            LOGGER.warn("Object already exists in repo: {}", pid);
            throw new DigitalObjectException(pid, "Object already exists in repo");
        }

        try {
            String foxml = client.downloadFoxml(pid, userProfile.getToken());
            saveToTmp(foxml, pid);

            if (Const.DIGITAL_OBJECT_MODEL_PAGE.equals(model)) {
                boolean hasAlto;
                try {
                    hasAlto = containsAlto(pid);
                } catch (Exception e) {
                    LOGGER.warn("Error checking ALTO for pid {}: {}", pid, e.getMessage());
                    hasAlto = false;
                }

                if (!hasAlto) {
                    try {
                        String alto = client.downloadAlto(pid, userProfile.getToken());
                        saveAltoToTmp(alto, pid);
                        updateFoxml(pid);
                    } catch (Exception e) {
                        LOGGER.error("Failed to download or update ALTO for pid {}: {}", pid, e.getMessage());
                        throw new AltoEditorException("ALTO processing failed for pid " + pid, e);
                    }
                }
            }

            importToStorage(pid, userProfile.getUsername());
        } finally {
            deleteFromTmp(pid);
        }

    }

    private void deleteFromTmp(String pid) throws AltoEditorException {
        File pidFile = getFile(pid);
        if (pidFile != null && pidFile.exists()) {
            deleteFolder(pidFile);
        }

        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        if (altoFile != null && altoFile.exists()) {
            deleteFolder(altoFile);
        }
    }

    private void updateFoxml(String pid) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);

        if (pidFile == null) {
            throw new IOException("FOXML file not found for pid: " + pid);
        }

        if (!pidFile.exists() || !pidFile.canRead()) {
            throw new IOException("Cannot read FOXML file: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalStorage.LocalObject lObj = localStorage.load(pid, pidFile);

        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        if (altoFile == null) {
            throw new IOException("ALTO file not found for pid: " + pid);
        }
        if (!altoFile.exists() || !altoFile.canRead() || !altoFile.canWrite()) {
            throw new IOException("Cannot read/write ALTO file: " + altoFile.getAbsolutePath());
        }

        AltoDatastreamEditor.importAlto(lObj, altoFile.toURI(), "Default ALTO", AltoDatastreamEditor.ALTO_ID + ".0");
        lObj.flush();
    }

    private boolean containsAlto(String pid) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);

        if (pidFile == null) {
            throw new IOException("FOXML file not found for pid: " + pid);
        }

        if (!pidFile.exists() || !pidFile.canRead()) {
            throw new IOException("Cannot read FOXML file: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalStorage.LocalObject lObj = localStorage.load(pid, pidFile);

        if (lObj != null && lObj.getDigitalObject() != null) {
            List<DatastreamType> datastreams = lObj.getDigitalObject().getDatastream();
            if (datastreams != null) {
                for (DatastreamType datastream : datastreams) {
                    if (AltoDatastreamEditor.ALTO_ID.equals(datastream.getID())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void importToStorage(String pid, String login) throws AltoEditorException, IOException {
        File pidFile = getFile(pid);

        if (pidFile == null) {
            throw new IOException("FOXML file not found for pid: " + pid);
        }

        if (!pidFile.exists() || !pidFile.canRead()) {
            throw new IOException("Cannot read FOXML file: " + pidFile.getAbsolutePath());
        }

        LocalStorage localStorage = new LocalStorage();
        LocalStorage.LocalObject lObj = localStorage.load(pid, pidFile);

        AkubraStorage akubraStorage = AkubraStorage.getInstance();
        akubraStorage.ingest(lObj, login, "Ingested by AltoEditor from Kramerius");
    }

    private void saveToTmp(String foxmlContent, String pid) throws AltoEditorException, IOException {
        File foxmlFile = getFile(pid);
        writeToFile(foxmlContent, foxmlFile, pid);
    }

    private void saveAltoToTmp(String altoContent, String pid) throws AltoEditorException, IOException {
        File altoFile = getFile(pid, AltoDatastreamEditor.ALTO_ID);
        writeToFile(altoContent, altoFile, pid);
    }

    public File saveImage(String pid, String instanceId, UserProfile userProfile, String model) throws AltoEditorException, IOException {
        return saveImage(pid, pid, instanceId, userProfile, model);
    }

    public File saveImage(String parentPid, String pid, String instanceId, UserProfile userProfile, String model) throws AltoEditorException, IOException {

        if (model == null || model.isEmpty()) {
            throw new IOException("Unknown model for pid = " + pid);
        }

        File parentFile = FileUtils.getPeroTmpFolder(model, parentPid, true);
        try (K7Client client = new K7Client(findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId))) {

            if (Const.DIGITAL_OBJECT_MODEL_PAGE.equals(model)) {
                File imageFile = getFile(parentFile, pid, "IMAGE");
                InputStream imageContent = client.getStream(pid, userProfile.getToken());
                writeToFile(imageContent, imageFile, pid);
            } else {
                List<String> childrenPids = client.getChildrenPids(pid, userProfile.getToken());
                for (String childrenPid : childrenPids) {
                    File imageFile = getFile(parentFile, childrenPid, "IMAGE");
                    InputStream imageContent = client.getStream(childrenPid, userProfile.getToken());
                    writeToFile(imageContent, imageFile, pid);
                }
            }
        }
        return parentFile;
    }

    public void uploadAltoOcr(DigitalObject digitalObject, UserProfile userProfile) throws AltoEditorException, IOException, InterruptedException {
        if (Const.DIGITAL_OBJECT_MODEL_OTHER.equals(digitalObject.getModel())) {
            uploadBatchAltoOcr(digitalObject, userProfile);
        } else {
            uploadAltoOcr(digitalObject.getPid(), digitalObject.getVersion(), digitalObject.getInstance(), userProfile);
        }
    }

    private void uploadBatchAltoOcr(DigitalObject digitalObject, UserProfile userProfile) throws AltoEditorException, IOException, InterruptedException {

        File parentFile = FileUtils.getPeroTmpFolder(digitalObject.getModel(), digitalObject.getPid(), false);

        if (parentFile == null || !parentFile.exists() || !parentFile.isDirectory()) {
            throw new DigitalObjectNotFoundException(digitalObject.getPid(), "Temporary folder not found for this digital object.");
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), digitalObject.getInstance());
        if (instance == null) {
            throw new DigitalObjectNotFoundException(digitalObject.getInstance(), String.format("This instance \"%s\" is not configured.", digitalObject.getInstance()));
        }

        File[] files = parentFile.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            throw new AltoEditorException("No files found in temporary folder: " + parentFile.getAbsolutePath());
        }

        try (K7Client client = new K7Client(instance)) {

            for (File file : files) {

                String filename = file.getName();
                int dotIndex = filename.lastIndexOf('.');
                if (dotIndex <= 0) {
                    LOGGER.warn("Skipping file without extension: {}", filename);
                    continue;
                }

                String extension = filename.substring(dotIndex + 1).toLowerCase();
                String uuid = "uuid:" + (filename.substring(0, dotIndex));

                if (!"xml".equals(extension) && !"txt".equals(extension)) {
                    continue;
                }

                try {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                    String datastreamType = "txt".equals(extension) ? Const.DATASTREAM_TYPE_OCR : Const.DATASTREAM_TYPE_ALTO;
                    client.uploadStream(uuid, datastreamType, content, userProfile.getToken());
                } catch (IOException e) {
                    throw new AltoEditorException("Failed to read or upload file: " + filename, e);
                }
            }

            client.indexDocument(digitalObject.getPid(), userProfile.getToken());
        }
    }

    public void uploadAltoOcr(String pid, String versionId, String instanceId, UserProfile userProfile) throws IOException, AltoEditorException, InterruptedException {
        String alto = getAltoFromRepository(pid, versionId);
        if (alto == null || alto.isBlank()) {
            throw new AltoEditorException(pid + "/" + versionId, "Alto to upload is null or empty");
        }
        String ocr = createOcrFromAlto(alto);
        if (ocr == null || ocr.isBlank()) {
            throw new AltoEditorException(pid + "/" + versionId, "OCR to upload is null or empty");
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        try (K7Client client = new K7Client(instance)) {
            client.uploadStream(pid, Const.DATASTREAM_TYPE_ALTO, alto, userProfile.getToken());
            client.uploadStream(pid, Const.DATASTREAM_TYPE_OCR, ocr, userProfile.getToken());
            client.indexDocument(pid, userProfile.getToken());
        }
    }

    private String getAltoFromRepository(String pid, String versionId) throws IOException, AltoEditorException {
        AkubraStorage storage = AkubraStorage.getInstance();
        AkubraStorage.AkubraObject object = storage.find(pid);
        AltoDatastreamEditor altoEditor = AltoDatastreamEditor.alto(object);
        return altoEditor.readRecordAsString(versionId);
    }
}
