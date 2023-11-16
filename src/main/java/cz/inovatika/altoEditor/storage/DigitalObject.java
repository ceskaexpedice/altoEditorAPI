package cz.inovatika.altoEditor.storage;

import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import cz.inovatika.altoEditor.editor.XmlStreamEditor;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import java.util.List;

public interface DigitalObject {

    String getPid();

    List<DatastreamProfile> getStreamProfile(String dsId, String versionId) throws AltoEditorException;

    XmlStreamEditor getEditor(DatastreamProfile datastream);

    void register(XmlStreamEditor editor);

    void setLabel(String label);

    void setModel(String modelId);

    String getModel();

    void flush() throws AltoEditorException;

    String asText() throws AltoEditorException;

    void purgeDatastream(String datastream, String logMessage) throws AltoEditorException;
}
