/*
 * Copyright (C) 2012 Jan Pokorsky
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.altoEditor.storage.local;

import com.yourmediashelf.fedora.generated.foxml.ContentLocationType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamVersionType;
import com.yourmediashelf.fedora.generated.foxml.DigitalObject;
import com.yourmediashelf.fedora.generated.foxml.PropertyType;
import com.yourmediashelf.fedora.generated.foxml.StateType;
import com.yourmediashelf.fedora.generated.foxml.XmlContentType;
import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import cz.inovatika.altoEditor.exception.StorageException;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectConcurrentModificationException;
import cz.inovatika.altoEditor.storage.AbstractDigitalObject;
import cz.inovatika.altoEditor.editor.XmlStreamEditor;
import cz.inovatika.altoEditor.editor.XmlStreamEditor.EditorResult;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FoxmlUtils;
import cz.inovatika.altoEditor.utils.FoxmlUtils.ControlGroup;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static cz.inovatika.altoEditor.utils.FoxmlUtils.createXmlDate;

/**
 * Local storage supporting FOXML 1.1.
 * It contains newly imported but not yet ingested files.
 *
 * @author Jan Pokorsky
 */
public final class LocalStorage {

    private static final Logger LOGGER = LogManager.getLogger(LocalStorage.class.getName());

    public LocalObject load(String pid, File foxml) {
        DigitalObject dobj = FoxmlUtils.unmarshal(new StreamSource(foxml), DigitalObject.class);
        LocalObject result = new LocalObject(pid, foxml, dobj);
        return result;
    }

    public LocalObject create() {
        return create((String) null, null);
    }
    
    public LocalObject create(String pid) {
        return create(pid, null);
    }

    public LocalObject create(DigitalObject dobj) {
        return create(null, dobj);
    }
    
    public LocalObject create(File foxml, DigitalObject dobj) {
        String pid = dobj.getPID();
        pid = pid != null ? pid : FoxmlUtils.createPid();
        return new LocalObject(pid, foxml, dobj);
    }

    public LocalObject create(File foxml) {
        return create(null, foxml);
    }
    
    public LocalObject create(String pid, File foxml) {
        pid = pid != null ? pid : FoxmlUtils.createPid();
        DigitalObject dobj = FoxmlUtils.createFoxml(pid);
        return new LocalObject(pid, foxml, dobj);
    }

    public static final class LocalObject extends AbstractDigitalObject {

        /** The helper property to mark a local FOXML as a copy of the remote object. */
        private static final String PROPERTY_REMOTE_COPY = "proarc:model#remoteCopy";
        private DigitalObject dobj;
        /** {@code null} for in memory object. */
        private File foxml;
        private String modelId;

        LocalObject(String pid, File foxml, DigitalObject dobj) {
            super(pid);
            this.foxml = foxml;
            if (dobj == null) {
                throw new NullPointerException("dobj");
            }
            this.dobj = dobj;
        }

        public String getOwner() {
            PropertyType p = FoxmlUtils.findProperty(dobj, Const.FOXML_PROPERTY_OWNER);
            return p == null ? null : p.getVALUE();
        }

        public String getLabel() {
            PropertyType p = FoxmlUtils.findProperty(dobj, Const.FOXML_PROPERTY_LABEL);
            return p == null ? null : p.getVALUE();
        }

        @Override
        public void setLabel(String label) {
            if (label == null) {
                throw new NullPointerException();
            } else if (label.length() > 255) {
                // length 255 is Fedora limit
                label = label.substring(0, 255);
            }
            FoxmlUtils.setProperty(dobj, Const.FOXML_PROPERTY_LABEL, label);
        }

        @Override
        public void setModel(String modelId) {
            this.modelId = modelId;
        }

        @Override
        public String getModel() {
            return this.modelId;
        }

        public void setOwner(String owner) {
            FoxmlUtils.setProperty(dobj, Const.FOXML_PROPERTY_OWNER, owner);
        }

        /** The helper property to mark a local FOXML as a copy of the remote object. */
        public void setRemoteCopy(boolean remote) {
            String val = remote ? Boolean.TRUE.toString() : null;
            FoxmlUtils.setProperty(dobj, PROPERTY_REMOTE_COPY, val);
        }

        /** The helper property to mark a local FOXML as a copy of the remote object. */
        public boolean isRemoteCopy() {
            return FoxmlUtils.findProperty(dobj, PROPERTY_REMOTE_COPY) != null;
        }

        @Override
        public void flush() throws StorageException {
            super.flush();
            if (foxml != null) {
                FoxmlUtils.marshal(new StreamResult(foxml), dobj, true);
            }
        }

        public File getFoxml() {
            return foxml;
        }

        @Override
        public XmlStreamEditor getEditor(DatastreamProfile datastream) {
            return new LocalXmlStreamEditor(this, datastream);
        }

        public DigitalObject getDigitalObject() {
            return dobj;
        }

        @Override
        public String asText() throws StorageException {
            try {
                return FoxmlUtils.toXml(dobj, true);
            } catch (Exception ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        @Override
        public void purgeDatastream(String datastream, String logMessage) throws StorageException {
            throw new UnsupportedOperationException(datastream);
        }

        @Override
        public List<DatastreamProfile> getStreamProfile(String dsId, String versionId) throws StorageException {
            List<DatastreamType> datastreams;
            if (dsId == null) {
                datastreams = dobj.getDatastream();
            } else {
                DatastreamType datastream = FoxmlUtils.findDatastream(dobj, dsId);
                datastreams = (datastream != null)
                        ? Arrays.asList(datastream)
                        : Collections.<DatastreamType>emptyList();
            }
            List<DatastreamProfile> profiles = new ArrayList<DatastreamProfile>(datastreams.size());
            for (DatastreamType datastream : datastreams) {
                profiles.add(FoxmlUtils.toDatastreamProfile(getPid(), datastream, versionId));
            }
            return profiles;
        }
    }

    public static final class LocalXmlStreamEditor implements XmlStreamEditor {

        private final LocalObject object;
        private DatastreamProfile defaultProfile;
        private final boolean isXml;

        private LocalXmlStreamEditor(LocalObject object, DatastreamProfile defaultProfile) {
            this.object = object;
            this.defaultProfile = defaultProfile;
            String mime = defaultProfile.getDsMIME();
            this.isXml = MediaType.TEXT_XML.equals(mime) || MediaType.APPLICATION_XML.equals(mime);
        }

        @Override
        public Source read(String versionId) {
            // find version
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), defaultProfile.getDsID(), versionId);
            return createSource(version);
        }

        private Source createSource(DatastreamVersionType version) {
            if (version == null) {
                return null;
            }
            XmlContentType xmlContent = version.getXmlContent();
            if (xmlContent != null) {
                Element elm = xmlContent.getAny().get(0);
                return new DOMSource(elm);
            } else {
                byte[] binaryContent = version.getBinaryContent();
                ContentLocationType contentLocation = version.getContentLocation();
                if (binaryContent != null) {
                    return new StreamSource(new ByteArrayInputStream(binaryContent));
                } else if (contentLocation != null) {
                    String ref = contentLocation.getREF();
                    if (ref != null) {
                        URI refUri = URI.create(ref);
                        return new StreamSource(new File(refUri));
                    }
                }
            }
            return null;
        }

        @Override
        public InputStream readStream(String versionId) throws StorageException {
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), defaultProfile.getDsID(), versionId);
            if (version != null) {
                byte[] binaryContent = version.getBinaryContent();
                ContentLocationType contentLocation = version.getContentLocation();
                if (binaryContent != null) {
                    return new ByteArrayInputStream(binaryContent);
                } else if (contentLocation != null) {
                    String ref = contentLocation.getREF();
                    if (ref != null) {
                        try {
                            URI refUri = new URI(ref);
                            return new FileInputStream(new File(refUri));
                        } catch (URISyntaxException ex) {
                            throw new StorageException(object.getPid(), ex);
                        } catch (FileNotFoundException ex) {
                            throw new StorageException(object.getPid(), ex);
                        }
                    }
                } else if (version.getXmlContent() != null) {
                    throw new StorageException(object.getPid(),
                            "XML inlined! Use read() method.");
                }
            }
            return null;
        }

        @Override
        public long getLastModified(String versionId) {
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), defaultProfile.getDsID(), versionId);
            long lastModified = getLastModified(version);
            return lastModified;
        }

        private long getLastModified(DatastreamVersionType version) {
            long last = Long.MIN_VALUE;
            if (version != null) {
                last = version.getCREATED().toGregorianCalendar().getTimeInMillis();
            }
            return last;
        }

        /**
         * Gets {@link DatastreamVersionType} as {@link DatastreamProfile} for
         * {@link #setProfile setProfile}. Only dsId, dsLabel, dsCreateDate,
         * dsFormatURI and dsMIME are translated.
         * @return the stream profile
         * @throws StorageException
         */
        @Override
        public DatastreamProfile getProfile(String versionId) throws StorageException {
            return getProfileImp(versionId);
        }

        public DatastreamProfile getProfileImp(String versionId) {
            String dsId = defaultProfile.getDsID();
            DatastreamType datastream = FoxmlUtils.findDatastream(object.getDigitalObject(), dsId);
            if (datastream == null) {
                return defaultProfile;
            } else {
                return FoxmlUtils.toDatastreamProfile(object.getPid(), datastream, versionId);
            }
        }

        /**
         * Updates {@link DatastreamVersionType} properties with {@link DatastreamProfile}
         * where it makes sense.
         * @param profile profile
         * @throws StorageException failure
         */
        @Override
        public void setProfile(DatastreamProfile profile, String versionId) throws StorageException {
            if (profile == null) {
                throw new NullPointerException();
            }
            String dsId = defaultProfile.getDsID();
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), dsId, versionId);
            if (version == null) {
                defaultProfile = profile;
                return ;
            }
            version.setCREATED(profile.getDsCreateDate());
            version.setFORMATURI(profile.getDsFormatURI());
            version.setLABEL(profile.getDsLabel());
            version.setMIMETYPE(profile.getDsMIME());
        }

        @Override
        public void write(EditorResult data, long timestamp, String message, String versionId) throws AltoEditorException {
            DatastreamVersionType version = getDatastreamVersionType(timestamp, versionId);
            if (data instanceof EditorBinaryResult) {
                version.setBinaryContent(null);
                version.setXmlContent(null);
            } else if (data instanceof EditorDomResult) {
                writeXmlContent(version, (EditorDomResult) data);
            } else {
                throw new StorageException(object.getPid(), "Unsupported data: " + data);
            }

            try {
                version.setCREATED(createXmlDate());
            } catch (DatatypeConfigurationException ex) {
                throw new StorageException(this.object.getPid(), ex);
            }
            object.register(this);
        }

        @Override
        public void write(byte[] data, long timestamp, String message, String versionId) throws AltoEditorException {
            writeBytesOrStream(data, null, timestamp, versionId);
        }

        @Override
        public void write(InputStream data, long timestamp, String message, String versionId) throws AltoEditorException {
            try {
                writeBytesOrStream(null, data, timestamp, versionId);
            } finally {
                FoxmlUtils.closeQuietly(data, object.getPid());
            }
        }

        @Override
        public void write(URI data, long timestamp, String message, String versionId) throws AltoEditorException {
            ControlGroup control = ControlGroup.fromExternal(defaultProfile.getDsControlGroup());
            if (control != ControlGroup.MANAGED) {
                throw new UnsupportedOperationException("Not supported yet: " + control);
            }
            DatastreamVersionType version = getDatastreamVersionType(timestamp, versionId);
            ContentLocationType contentLocation = new ContentLocationType();
            contentLocation.setTYPE("URL");
            contentLocation.setREF(data.toASCIIString());
            version.setContentLocation(contentLocation);
            version.setBinaryContent(null);
            try {
                version.setCREATED(createXmlDate());
            } catch (DatatypeConfigurationException ex) {
                throw new StorageException(this.object.getPid(), ex);
            }
            object.register(this);
        }
        
        @Override
        public EditorResult createResult(String versionId) {
            if (!isXml) {
                throw new UnsupportedOperationException("requires */xml mime type");
            }
            String reference = getReference(versionId);
            return reference == null ? new EditorDomResult() : new EditorBinaryResult(reference);
        }

        @Override
        public void flush() {
            // no op
        }

        private String getReference(String versionId) {
            String dsId = defaultProfile.getDsID();
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), dsId, versionId);
            if (version != null && version.getContentLocation() != null) {
                return version.getContentLocation().getREF();
            }
            return null;
        }

        private DatastreamVersionType getDatastreamVersionType(long timestamp, String versionId) throws DigitalObjectConcurrentModificationException {
            String dsId = defaultProfile.getDsID();
            DatastreamVersionType version = FoxmlUtils.findDataStreamVersion(object.getDigitalObject(), dsId, versionId);
            if (version == null) {
                ControlGroup cgroup = ControlGroup.fromExternal(defaultProfile.getDsControlGroup());
                version = FoxmlUtils.createDataStreamVersion(
                        object.getDigitalObject(), dsId, cgroup, Boolean.valueOf(defaultProfile.getDsVersionable()), StateType.A, versionId);
                version.setMIMETYPE(defaultProfile.getDsMIME());
                version.setLABEL(defaultProfile.getDsLabel());
                version.setFORMATURI(defaultProfile.getDsFormatURI());
            } else if (timestamp != getLastModified(version)) {
                throw new DigitalObjectConcurrentModificationException(object.getPid(), dsId);
            }
            return version;
        }

        private void writeXmlContent(DatastreamVersionType version, EditorDomResult data) {
            XmlContentType xmlContent = new XmlContentType();
            Node root = data.getNode();
            Document doc = root.getOwnerDocument() == null ? (Document) root : root.getOwnerDocument();
            xmlContent.getAny().add(doc.getDocumentElement());
            version.setXmlContent(xmlContent);
        }

        private void writeBytesOrStream(byte[] bytes, InputStream stream, long timestamp, String versionId) throws AltoEditorException {
            ControlGroup control = ControlGroup.fromExternal(defaultProfile.getDsControlGroup());
            if (control != ControlGroup.MANAGED) {
                throw new UnsupportedOperationException("Not supported yet: " + control);
            }
            DatastreamVersionType version = getDatastreamVersionType(timestamp, versionId);
            String reference = getReference(versionId);
            boolean storeExternally = reference != null;
            if (storeExternally) {
                writeBytesOrStreamExternally(version, bytes, stream, reference);
            } else {
                if (bytes != null) {
                    version.setBinaryContent(bytes);
                } else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
                    try {
                        FoxmlUtils.copy(stream, baos);
                        version.setBinaryContent(baos.toByteArray());
                    } catch (IOException ex) {
                        throw new StorageException(object.getPid(), ex);
                    }
                }
                version.setContentLocation(null);
            }
            try {
                version.setCREATED(createXmlDate());
            } catch (DatatypeConfigurationException ex) {
                throw new StorageException(this.object.getPid(), ex);
            }
            object.register(this);
        }

        private void writeBytesOrStreamExternally(DatastreamVersionType version,
                byte[] bytes, InputStream stream, String reference
                ) throws StorageException {

            FileOutputStream fos = null;
            try {
                URI ref = new URI(reference);
                File file = new File(ref);
                fos = new FileOutputStream(file);
                if (bytes != null) {
                    fos.write(bytes);
                } else {
                    FoxmlUtils.copy(stream, fos);
                }
                version.setBinaryContent(null);
            } catch (URISyntaxException ex) {
                throw new StorageException(object.getPid(), ex);
            } catch (IOException ex) {
                throw new StorageException(object.getPid(), ex);
            } finally {
                FoxmlUtils.closeQuietly(fos, object.getPid());
            }
        }

        @Override
        public String toString() {
            return String.format("%s{pid=%s, dsId=%s, mimetype=%s, controlGroup=%s,\nfoxml=%s}",
                    getClass().getSimpleName(),
                    object.getPid(),
                    defaultProfile.getDsID(),
                    getProfileImp("0").getDsMIME(),
                    defaultProfile.getDsControlGroup(),
                    object.getFoxml());
        }
    }

    private static final class EditorDomResult extends DOMResult implements EditorResult {
    }

    private static final class EditorBinaryResult extends StreamResult implements EditorResult {

        public EditorBinaryResult(String reference) {
            super(reference);
        }

    }

}
