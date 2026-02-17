package cz.inovatika.altoEditor.kramerius;

import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage;
import cz.inovatika.altoEditor.storage.local.LocalStorage.LocalObject;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.FileUtils.deleteFolder;
import static cz.inovatika.altoEditor.utils.FileUtils.getFile;
import static cz.inovatika.altoEditor.utils.FileUtils.writeToFile;

/**
 * The K7Downloader class provides functionality to download, update, and store
 * FOXML data and associated ALTO files from Kramerius digital repositories.
 * It interacts with Kramerius repositories via the K7Client and handles various
 * digital object operations such as downloading, temporary storage, ALTO file
 * management, and ingestion into a local repository.
 *
 * This class includes error handling mechanisms to ensure proper handling of
 * edge cases like missing or invalid files, and provides logging for monitoring
 * the processes.
 */
public class K7Downloader {

    private static final Logger LOGGER = LogManager.getLogger(K7Downloader.class.getName());

    public void downloadFoxml(String pid, String model, String instanceId, UserProfile userProfile) throws AltoEditorException, IOException {
        AkubraStorage storage = AkubraStorage.getInstance();
        if (storage.exist(pid)) {
            LOGGER.warn("Object already exists in repo: {}", pid);
            throw new DigitalObjectException(pid, "Object already exists in repo");
        }

        KrameriusOptions.KrameriusInstance instance = findKrameriusInstance(KrameriusOptions.get().getKrameriusInstances(), instanceId);
        if (instance == null) {
            throw new DigitalObjectNotFoundException(instanceId, String.format("This instance \"%s\" is not configured.", instanceId));
        }

        try (K7Client client = new K7Client(instance)) {
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
        LocalObject lObj = localStorage.load(pid, pidFile);

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
        LocalObject lObj = localStorage.load(pid, pidFile);

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
        LocalObject lObj = localStorage.load(pid, pidFile);

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
                K7ObjectInfo k7ObjectInfo = new K7ObjectInfo();
                int childCount = k7ObjectInfo.getChildrenPidSize(pid, instanceId, userProfile);
                List<String> childrenPids = k7ObjectInfo.getChildrenPids(pid, instanceId, userProfile, childCount);
                for (String childrenPid : childrenPids) {
                    File imageFile = getFile(parentFile, childrenPid, "IMAGE");
                    InputStream imageContent = client.getStream(childrenPid, userProfile.getToken());
                    writeToFile(imageContent, imageFile, pid);
                }
            }
        }
        return parentFile;
    }
}
