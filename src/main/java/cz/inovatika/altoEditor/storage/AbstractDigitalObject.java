package cz.inovatika.altoEditor.storage;

import cz.inovatika.altoEditor.editor.XmlStreamEditor;
import cz.inovatika.altoEditor.exception.StorageException;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractDigitalObject implements DigitalObject {

    private final String pid;

    private final Set<XmlStreamEditor> editors = new LinkedHashSet<XmlStreamEditor>();

    public AbstractDigitalObject(String pid) {
        this.pid = pid;
    }

    @Override
    public final String getPid() {
        return pid;
    }

    @Override
    public final void register(XmlStreamEditor editor) {
        editors.add(editor);
    }

    @Override
    public void flush() throws StorageException {
        // write changes
        for (XmlStreamEditor editor : editors) {
            editor.flush();
        }
    }

}
