package cz.inovatika.altoEditor.utils;

import cz.inovatika.altoEditor.editor.AltoDatastreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.kramerius.K7Downloader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cz.inovatika.altoEditor.utils.FoxmlUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class.getName());

    public static String getPidAsFile(String value) {
        if (value.startsWith("uuid:")) {
            return value.substring(5);
        }
        return value;
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static File getFile(String pid) throws AltoEditorException {
        return getFile(new File(Utils.getDefaultHome(), "tmp"), pid, null);
    }

    public static File getFile(String pid, String stream) throws AltoEditorException {
        return getFile(new File(Utils.getDefaultHome(), "tmp"), pid, stream);
    }

    public static File getFile(File parentFolder, String pid, String stream) throws AltoEditorException {
        parentFolder = createFolder(parentFolder, false);
        File pidFile;
        String extension = getExtension(stream);
        if (stream != null && !stream.isEmpty() && !"IMAGE".equals(stream)) {
            pidFile = new File(parentFolder, stream + "_" + getPidAsFile(pid) + "." + extension);
        } else {
            pidFile = new File(parentFolder, getPidAsFile(pid) + "." + extension);
        }
        return pidFile;
    }

    private static String getExtension(String stream) {
        if (isBlank(stream) || AltoDatastreamEditor.ALTO_ID.equals(stream)) {
            return "xml";
        } else if ("IMAGE".equals(stream)) {
            return "jpg";
        }
        return "xml";
    }

    public static File createFolder(File folder, boolean deleteIfExists) throws AltoEditorException {
        if (folder.exists()) {
            if (deleteIfExists) {
                deleteFolder(folder);
            }
        }
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                LOGGER.error("It is not possible to create " + folder.getName() + " folder in " + folder.getParentFile().getAbsolutePath());
                throw new AltoEditorException("It is not possible to create " + folder.getName() + " folder in " + folder.getParentFile().getAbsolutePath());
            }
        }
        return folder;
    }

    public static void writeToFile(String content, File file, String pid) throws IOException {
        if (file.exists()) {
            deleteFolder(file);
        }

        if (!file.createNewFile()) {
            LOGGER.warn("Can not create file " + file.getAbsolutePath());
            throw new IOException("Nepodařilo se vytvořit soubor " + file.getAbsolutePath());
        }

        FileOutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(content);
        } finally {
            closeQuietly(outputStreamWriter, pid);
            closeQuietly(outputStream, pid);
        }
    }

    public static void writeToFile(InputStream content, File file, String pid) throws IOException {
        if (file.exists()) {
            deleteFolder(file);
        }

        if (!file.createNewFile()) {
            LOGGER.warn("Can not create file " + file.getAbsolutePath());
            throw new IOException("Nepodařilo se vytvořit soubor " + file.getAbsolutePath());
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            IOUtils.copy(content, outputStream);
        } finally {
            closeQuietly(outputStream, pid);
        }
    }
}
