package cz.inovatika.altoEditor.editor;

import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.StorageException;
import java.io.InputStream;
import java.net.URI;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

public interface XmlStreamEditor {

    EditorResult createResult(String versionId);

    long getLastModified(String versionId) throws AltoEditorException;

    DatastreamProfile getProfile(String versionId) throws AltoEditorException;

    void setProfile(DatastreamProfile profile, String versionId) throws AltoEditorException;

    Source read(String versionId) throws StorageException;

    InputStream readStream(String versionId) throws StorageException;

    void write(EditorResult data, long timestamp, String message, String versionId) throws AltoEditorException;

    void write(byte[] data, long timestamp, String message, String versionId) throws AltoEditorException;

    void write(URI data, long timestamp, String message, String versionId) throws AltoEditorException;

    void write(InputStream data, long timestamp, String message, String versionId) throws AltoEditorException;

    void flush() throws StorageException;

    public interface EditorResult extends Result {
    }
}
