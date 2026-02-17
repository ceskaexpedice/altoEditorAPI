package cz.inovatika.altoEditor.kramerius;

import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.storage.akubra.AkubraStorage;
import cz.inovatika.altoEditor.user.UserProfile;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cz.inovatika.altoEditor.kramerius.KrameriusOptions.findKrameriusInstance;
import static cz.inovatika.altoEditor.utils.OcrUtils.createOcrFromAlto;

/**
 * The K7Uploader class provides functionality for uploading OCR data and ALTO files
 * related to digital objects to an external repository system.
 *
 * This class supports both individual and batch uploads depending on the model of
 * the digital object being processed. It integrates with repository APIs and handles
 * the transformation of ALTO data to OCR where applicable.
 *
 * Features:
 * - Handles the upload of ALTO and OCR data streams to the repository.
 * - Supports batch processing of multiple files from a temporary storage folder.
 * - Configures repository access using instance-specific details.
 * - Validates and processes files before uploading, ensuring that incompatible file types are skipped.
 * - Ensures proper indexing of uploaded content within the repository.
 */
public class K7Uploader {

    private static final Logger LOGGER = LogManager.getLogger(K7Uploader.class.getName());

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
