package cz.inovatika.altoEditor.storage.akubra;

import com.yourmediashelf.fedora.generated.foxml.ContentLocationType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamVersionType;
import com.yourmediashelf.fedora.generated.foxml.DigitalObject;
import com.yourmediashelf.fedora.generated.foxml.ObjectPropertiesType;
import com.yourmediashelf.fedora.generated.foxml.PropertyType;
import com.yourmediashelf.fedora.generated.foxml.StateType;
import com.yourmediashelf.fedora.generated.foxml.XmlContentType;
import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import com.yourmediashelf.fedora.util.DateUtility;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectConcurrentModificationException;
import cz.inovatika.altoEditor.exception.DigitalObjectNotFoundException;
import cz.inovatika.altoEditor.exception.StorageException;
import cz.inovatika.altoEditor.exception.DigitalObjectExistException;
import cz.inovatika.altoEditor.storage.AbstractDigitalObject;
import cz.inovatika.altoEditor.editor.XmlStreamEditor;
import cz.inovatika.altoEditor.storage.local.LocalStorage.LocalObject;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FoxmlUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.crypto.Data;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static cz.inovatika.altoEditor.utils.FoxmlUtils.createXmlDate;
import static cz.inovatika.altoEditor.utils.FoxmlUtils.getDatastream;

public class AkubraStorage {

    private static final Logger LOG = Logger.getLogger(AkubraStorage.class.getName());
    private XPathFactory xPathFactory;
    private static AkubraStorage INSTANCE;
    private AkubraManager manager;

    public static AkubraStorage getInstance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new AkubraStorage();
        }
        return INSTANCE;
    }

    public AkubraStorage() throws IOException {
        this.xPathFactory = XPathFactory.newInstance();
        try {
            this.manager = new AkubraManager();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public AkubraObject find(String pid) {
        return new AkubraObject(manager, pid);
    }

    public boolean exist(String pid) throws StorageException {
        return this.manager.objectExists(pid);
    }

    public void ingest(File foxml, String pid, String ingestUser, String log) throws StorageException {
        if (ingestUser == null || ingestUser.isEmpty()) {
            throw new IllegalArgumentException("ingestUser");
        }
        if (pid == null || pid.isEmpty()) {
            throw new IllegalArgumentException("PID is null or does not exists.");
        }
        if (foxml == null || !foxml.exists()) {
            throw new IllegalArgumentException("Foxml is null or does not exists.");
        }
        try {
            if (exist(pid)) {
                throw new DigitalObjectExistException(pid, null, "Object with PID " + pid + " already exists!", null);
            }


            InputStream inputStream = new FileInputStream(foxml);
            this.manager.addOrReplaceObject(pid, inputStream);

            LOG.log(Level.FINE, "Object with PID {0} added to repository.", pid);
        } catch (LowlevelStorageException | IOException | DigitalObjectExistException e) {
            throw new StorageException(pid, "Error during adding new object", e);
        }
    }

    public void ingest(LocalObject object, String ingestUser) throws StorageException, DigitalObjectExistException {
        ingest(object, ingestUser, "Ingested locally");
    }

    public void ingest(LocalObject object, String ingestUser, String log) throws StorageException, DigitalObjectExistException {
        if (ingestUser == null || ingestUser.isEmpty()) {
            throw new IllegalArgumentException("ingestUser");
        }
        if (log == null || log.isEmpty()) {
            throw new IllegalArgumentException("log");
        }
        if (object == null) {
            throw new IllegalArgumentException("Local Object is null or does not exists.");
        }
        if (object.getOwner() == null) {
            object.setOwner(ingestUser);
        }
        try {
            if (exist(object.getPid())) {
                throw new DigitalObjectExistException(object.getPid(), null, "Object with PID " + object.getPid() + " already exists!", null);
            }

            DigitalObject digitalObject = object.getDigitalObject();
            processStreams(digitalObject);
            updateProperties(digitalObject);
            String xml = FoxmlUtils.toXml(digitalObject, false);
            InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            this.manager.addOrReplaceObject(object.getPid(), inputStream);

            LOG.log(Level.FINE, "Object with PID {0} added to repository.", object.getPid());
        } catch (TransformerException | URISyntaxException | LowlevelStorageException | IOException e) {
            throw new StorageException(object.getPid(), "Error during adding new object", e);
        }
    }

    private void processStreams(DigitalObject digitalObject) throws IOException, LowlevelStorageException, URISyntaxException, TransformerException {
        for (DatastreamType datastream : digitalObject.getDatastream()) {
            if (FoxmlUtils.ControlGroup.MANAGED.toExternal().equals(datastream.getCONTROLGROUP())) {
                for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
                    if (datastreamVersion.getContentLocation() != null && "URL".equals(datastreamVersion.getContentLocation().getTYPE())) {
                        File inputFile = new File(new URI(datastreamVersion.getContentLocation().getREF()).getPath());
                        if (inputFile.exists()) {
                            InputStream inputStream = null;
                            try {
                                inputStream = new FileInputStream(inputFile);
                                String ref = digitalObject.getPID() + "+" + datastream.getID() + "+" + datastreamVersion.getID();
                                this.manager.addOrReplaceDatastream(ref, inputStream);
                                ContentLocationType contentLocationType = new ContentLocationType();
                                contentLocationType.setTYPE("INTERNAL_ID");
                                contentLocationType.setREF(ref);
                                datastreamVersion.setContentLocation(contentLocationType);
                            } finally {
                                inputStream.close();
                            }
                        }
                    }
                }
                for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
                    if (datastreamVersion.getXmlContent() != null && datastreamVersion.getXmlContent().getAny() != null && !datastreamVersion.getXmlContent().getAny().isEmpty()) {
                        Element element = datastreamVersion.getXmlContent().getAny().get(0);
                        StringWriter output = new StringWriter();

                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
                        transformer.transform(new DOMSource(element), new StreamResult(output));

                        String elementValue = output.toString();
//                        DOMImplementationLS lsImpl = (DOMImplementationLS)element.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
//                        LSSerializer serializer = lsImpl.createLSSerializer();
//                        lsImpl.createLSOutput();
//                        //serializer.getDomConfig().setParameter("xml-declaration", true); //by default its true, so set it to false to get String without xml-declaration
//                        String elementValue = serializer.writeToString(element);
                        InputStream inputStream = new ByteArrayInputStream(elementValue.getBytes(StandardCharsets.UTF_8));
                        String ref = digitalObject.getPID() + "+" + datastream.getID() + "+" + datastreamVersion.getID();
                        this.manager.addOrReplaceDatastream(ref, inputStream);
                        ContentLocationType contentLocationType = new ContentLocationType();
                        contentLocationType.setTYPE("INTERNAL_ID");
                        contentLocationType.setREF(ref);
                        datastreamVersion.setContentLocation(contentLocationType);
                        datastreamVersion.setXmlContent(null);
                    }
                }
            }
        }
    }

    public void updateProperties(DigitalObject object) {
        FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
        FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_STATE, Const.FOXML_PROPERTY_STATE_ACTIVE);
    }

    public static final class AkubraObject extends AbstractDigitalObject {

        private String label;
        private String modelId;
        private AkubraManager manager;

        public AkubraObject(AkubraManager manager, String pid) {
            super(pid);
            this.manager = manager;
        }

        @Override
        public XmlStreamEditor getEditor(DatastreamProfile datastream) {
            return new AkubraXmlStreamEditor(this, datastream);
        }

        @Override
        public void setLabel(String label) {
            if (label == null) {
                throw new NullPointerException();
            } else if (label.length() > 255) {
                label = label.substring(0, 255);
            }
            this.label = label;
        }

        @Override
        public void setModel(String modelId) {
            this.modelId = modelId;
        }

        @Override
        public String getModel() {
            return this.modelId;
        }

        @Override
        public void flush() throws StorageException {
            super.flush();
            try {
                DigitalObject object = this.manager.readObjectFromStorage(getPid());
                if (label != null) {
                    object = updateLabel(object, label);
                }
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
                if (object == null) {
                    throw new StorageException(getPid(), "Object " + getPid() + "is can not be flushed to Low-Level storage.");
                } else {
                    InputStream inputStream = this.manager.marshallObject(object);
                    this.manager.addOrReplaceObject(object.getPID(), inputStream);
                }
            } catch (
                    Exception ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        private DigitalObject updateLabel(DigitalObject object, String label) {
            if (object != null) {
                ObjectPropertiesType propertiesType = object.getObjectProperties();
                if (propertiesType != null) {
                    for (PropertyType property : propertiesType.getProperty()) {
                        if (Const.FOXML_PROPERTY_LABEL.equals(property.getNAME())) {
                            property.setVALUE(label);
                        }
                    }
                }
                return object;
            }
            return null;
        }

        public void delete(String logMessage) throws StorageException {
            try {
                DigitalObject object = this.manager.readObjectFromStorage(getPid());
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_STATE, Const.FOXML_PROPERTY_STATE_DEACTIVE);
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
                if (object == null) {
                    throw new StorageException(getPid(), "Object " + getPid() + "is can not be flushed to Low-Level storage.");
                } else {
                    InputStream inputStream = this.manager.marshallObject(object);
                    this.manager.addOrReplaceObject(object.getPID(), inputStream);
                }
            } catch (Exception ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        public void restore(String logMessage) throws StorageException {
            try {
                DigitalObject object = this.manager.readObjectFromStorage(getPid());
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_STATE, Const.FOXML_PROPERTY_STATE_ACTIVE);
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
                if (object == null) {
                    throw new StorageException(getPid(), "Object " + getPid() + "is can not be flushed to Low-Level storage.");
                } else {
                    InputStream inputStream = this.manager.marshallObject(object);
                    this.manager.addOrReplaceObject(object.getPID(), inputStream);
                }
            } catch (Exception ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        public void purge(String logMessage) throws StorageException {
            try {
                this.manager.deleteObject(getPid(), true);
            } catch (IOException ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        @Override
        public void purgeDatastream(String datastream, String logMessage) throws StorageException {
            try {
                this.manager.deleteStream(getPid(), datastream);
            } catch (IOException ex) {
                throw new StorageException(getPid(), ex);
            }
        }

        @Override
        public String asText() throws StorageException, DigitalObjectNotFoundException {
            try {
                InputStream stream = this.manager.retrieveObject(getPid());
                String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                return text;
            } catch (IOException ex) {
                if (ex.getMessage().contains("Object not found in low-level storage")) {
                    throw new DigitalObjectNotFoundException(getPid(), ex);
                } else {
                    throw new StorageException(getPid(), ex);
                }
            }
        }

        @Override
        public List<DatastreamProfile> getStreamProfile(String dsId, String versionId) throws StorageException {
            if (dsId == null) {
                return getDatastreamProfiles(versionId);
            } else {
                return getDatastreamProfile(dsId, versionId);
            }
        }

        private List<DatastreamProfile> getDatastreamProfile(String dsId, String versionId) throws StorageException {
            DigitalObject object = this.manager.readObjectFromStorage(getPid());
            DatastreamProfile profile = FoxmlUtils.getDatastreamProfile(object, dsId, versionId);
            return Collections.singletonList(profile);
        }

        public List<DatastreamProfile> getDatastreamProfiles(String versionId) throws StorageException {
            DigitalObject object = this.manager.readObjectFromStorage(getPid());
            return FoxmlUtils.getDatastreamProfiles(object, versionId);
        }

        public AkubraManager getManager() {
            return manager;
        }
    }

    public static final class AkubraXmlStreamEditor implements XmlStreamEditor {

        private final AkubraObject object;
        private final AkubraManager manager;
        private final String dsId;
        private long lastModified;
        private DatastreamProfile profile;
        private DatastreamProfile newProfile;
        private DatastreamContent data;
        private boolean modified;
        private String versionId;
        private boolean missingDataStream;
        private final DatastreamProfile defaultProfile;
        private String logMessage;

        public AkubraXmlStreamEditor(AkubraObject object, DatastreamProfile defaultProfile) {
            if (object == null) {
                throw new NullPointerException("object");
            }
            this.object = object;
            this.manager = object.getManager();
            defaultProfile.setPid(object.getPid());
            if (defaultProfile.getDsCreateDate() == null) {
                defaultProfile.setDsCreateDate(FoxmlUtils.createDate());
            }
            this.defaultProfile = defaultProfile;
            this.dsId = defaultProfile.getDsID();
        }

        public AkubraXmlStreamEditor(AkubraObject object, String dsId, DatastreamProfile defaultProfile) {
            this.object = object;
            this.manager = object.getManager();
            this.dsId = dsId;
            if (defaultProfile.getDsCreateDate() == null) {
                defaultProfile.setDsCreateDate(FoxmlUtils.createDate());
            }
            this.defaultProfile = defaultProfile;
        }


        private String toLogString() {
            return String.format("%s/%s, lastModified: %s (%s)", object.getPid(), dsId, lastModified,
                    DateUtility.getXSDDateTime(new Date(lastModified)));
        }

        private String toLogString(String message) {
            return String.format("%s/%s, lastModified: %s (%s), %s", object.getPid(), dsId, lastModified,
                    DateUtility.getXSDDateTime(new Date(lastModified)), message);
        }

        @Override
        public Source read(String versionId) throws StorageException {
            try {
                fetchData(versionId);
                return data == null ? null : data.asSource();
            } catch (Exception ex) {
                throw new StorageException(object.getPid(), toLogString(), ex);
            }
        }

        @Override
        public InputStream readStream(String versionId) throws StorageException {
            try {
                fetchData(versionId);
                return data == null ? null : data.asInputStream();
            } catch (Exception ex) {
                throw new StorageException(object.getPid(), toLogString(), ex);
            }
        }

        @Override
        public long getLastModified(String versionId) throws AltoEditorException {
            try {
                fetchProfile(versionId);
                return lastModified;
            } catch (Exception ex) {
                throw new StorageException(object.getPid(), toLogString(), ex);
            }
        }

        @Override
        public DatastreamProfile getProfile(String versionId) throws AltoEditorException {
            fetchProfile(versionId);
            return newProfile != null ? newProfile : profile;
        }

        @Override
        public void setProfile(DatastreamProfile profile, String versionId) throws AltoEditorException {
            this.newProfile = profile;
            object.register(this);
            modified = true;
            this.versionId = versionId;
        }


        @Override
        public void write(EditorResult data, long timestamp, String message, String versionId) throws AltoEditorException {
            if (data instanceof EditorStreamResult) {
                EditorStreamResult result = (EditorStreamResult) data;
                write(new DatastreamContent(result.asBytes()), timestamp, message, versionId);
            } else if (data instanceof EditorDomResult) {
                write(new DatastreamContent((EditorDomResult) data), timestamp, message, versionId);
            } else {
                throw new IllegalArgumentException("Unsupported data: " + data);
            }
        }

        @Override
        public void write(byte[] data, long timestamp, String message, String versionId) throws AltoEditorException {
            write(new DatastreamContent(data), timestamp, message, versionId);

        }

        @Override
        public void write(URI data, long timestamp, String message, String versionId) throws AltoEditorException {
            write(new DatastreamContent(data), timestamp, message, versionId);

        }

        @Override
        public void write(InputStream data, long timestamp, String message, String versionId) throws AltoEditorException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                FoxmlUtils.copy(data, byteArrayOutputStream);
            } catch (IOException ex) {
                throw new StorageException(object.getPid(), toLogString(), ex);
            } finally {
                FoxmlUtils.closeQuietly(data, toLogString());
            }
            write(new DatastreamContent(byteArrayOutputStream.toByteArray()), timestamp, message, versionId);
        }

        @Override
        public EditorResult createResult(String versionId) {
            return new EditorStreamResult();
        }

        @Override
        public void flush() throws StorageException {
            if (!modified) {
                return;
            }
            try {

                if (newProfile != null && !newProfile.getDsControlGroup().equals(profile.getDsControlGroup())) {
                    purgeDatastream(profile);
                    missingDataStream = true;
                }
//                if (missingDataStream) {
//                    addDatastreamVersion();
//                } else {
                    modifyDatastream();
//                }
                missingDataStream = false;
                modified = false;
                logMessage = null;
                newProfile = null;
                DigitalObject digitalObject = this.manager.readObjectFromStorage(this.object.getPid());
                profile = FoxmlUtils.getDatastreamProfile(digitalObject, dsId, versionId);
                lastModified = FoxmlUtils.getLastModified(digitalObject, dsId, versionId);
            } catch (Exception ex) {
                throw new StorageException(object.getPid(), toLogString(), ex);
            }
        }

        private void modifyDatastream() throws Exception, StorageException {
            DatastreamProfile profile = newProfile != null ? newProfile : this.profile;
            DigitalObject object = this.manager.readObjectFromStorage(this.object.getPid());

            DatastreamType datastreamType = getDatastream(object, profile);
            if (datastreamType == null) {
                throw new StorageException(object.getPID(), toLogString("Missing datastream."));
            }

            datastreamType = modifyDatastream(datastreamType, profile);

            object = replaceDatastream(object, datastreamType);

            InputStream inputStream = this.manager.marshallObject(object);
            this.manager.addOrReplaceObject(object.getPID(), inputStream);
            //this.manager.commit(object, profile.getDsID());
        }

        private DigitalObject replaceDatastream(DigitalObject object, DatastreamType datastreamType) {

            ListIterator<DatastreamType> iterator = object.getDatastream().listIterator();
            while (iterator.hasNext()) {
                if (iterator.next().getID().equals(datastreamType.getID())) {
                    iterator.remove();
                    break;
                }
            }
            object.getDatastream().add(datastreamType);
            return object;
        }

        private void addDatastream() throws Exception {
            DatastreamProfile profile = newProfile != null ? newProfile : this.profile;

            DigitalObject object = this.manager.readObjectFromStorage(this.object.getPid());
            DatastreamType datastreamType = createNewDatastream(profile);
            object.getDatastream().add(datastreamType);
            InputStream inputStream = this.manager.marshallObject(object);
            this.manager.addOrReplaceObject(object.getPID(), inputStream);
            //this.manager.commit(object, profile.getDsID());
        }

        private DatastreamType modifyDatastream(DatastreamType datastreamType, DatastreamProfile profile) throws IOException, StorageException, ParserConfigurationException, SAXException, LowlevelStorageException {
            datastreamType.setCONTROLGROUP(profile.getDsControlGroup());
            datastreamType.setID(profile.getDsID());

            if (datastreamType.getDatastreamVersion().isEmpty()) {
                throw new StorageException(object.getPid(), toLogString("Missing  datastreamVersionType for " + datastreamType.getID()));
            }
            if (!datastreamType.isVERSIONABLE() && datastreamType.getDatastreamVersion().size() > 1) {
                throw new StorageException(object.getPid(), toLogString("More than 1 datastreamVersionType for " + datastreamType.getID()));
            }

            DatastreamVersionType datastreamVersion = null;

            for (DatastreamVersionType version : datastreamType.getDatastreamVersion()) {
                if (this.versionId.equals(version.getID())) {
                    datastreamVersion = version;
                }
            }

            if (datastreamVersion == null) {
                datastreamVersion = new DatastreamVersionType();
                datastreamType.getDatastreamVersion().add(datastreamVersion);
                datastreamVersion.setID(this.versionId);
            }

            datastreamVersion.setLABEL(profile.getDsLabel());
            try {
                datastreamVersion.setCREATED(createXmlDate());
            } catch (DatatypeConfigurationException ex) {
                throw new StorageException(this.object.getPid(), toLogString(), ex);
            }
            datastreamVersion.setFORMATURI(profile.getDsFormatURI());
            datastreamVersion.setMIMETYPE(profile.getDsMIME());

            if (this.data != null) {
                FoxmlUtils.ControlGroup controlGroup = FoxmlUtils.ControlGroup.fromExternal(profile.getDsControlGroup());
                if (controlGroup == FoxmlUtils.ControlGroup.INLINE) {
                    datastreamVersion.setXmlContent(data.asXmlContent());
                } else if (controlGroup == FoxmlUtils.ControlGroup.MANAGED) {
//                    datastreamVersionType.setBinaryContent(IOUtils.toByteArray(this.data.asInputStream()));
                    String ref = object.getPid() + "+" + datastreamType.getID() + "+" + datastreamVersion.getID();
                    this.manager.addOrReplaceDatastream(ref, new ByteArrayInputStream(IOUtils.toByteArray(this.data.asInputStream())));
                    datastreamVersion.setBinaryContent(null);
                    ContentLocationType contentLocationType = new ContentLocationType();
                    contentLocationType.setTYPE("INTERNAL_ID");
                    contentLocationType.setREF(ref);
                    datastreamVersion.setContentLocation(contentLocationType);
                } else if (controlGroup == FoxmlUtils.ControlGroup.EXTERNAL) {
                    ContentLocationType contentLocation = new ContentLocationType();
                    datastreamVersion.setContentLocation(contentLocation);
                    contentLocation.setREF(this.data.reference.toASCIIString());
                    contentLocation.setTYPE("URL");
                } else {
                    throw new UnsupportedOperationException("DsControlGroup: " + controlGroup + "; " + toLogString());
                }
            }
            return datastreamType;
        }

        private DatastreamType createNewDatastream(DatastreamProfile profile) throws IOException, ParserConfigurationException, SAXException, LowlevelStorageException {
            DatastreamType datastreamType = new DatastreamType();
            datastreamType.setCONTROLGROUP(profile.getDsControlGroup());
            datastreamType.setID(profile.getDsID());
            datastreamType.setSTATE(StateType.A);
            datastreamType.setVERSIONABLE(Boolean.parseBoolean(profile.getDsVersionable()));

            DatastreamVersionType datastreamVersionType = new DatastreamVersionType();
            datastreamType.getDatastreamVersion().add(datastreamVersionType);

            datastreamVersionType.setID(profile.getDsVersionID());
            datastreamVersionType.setLABEL(profile.getDsLabel());
            datastreamVersionType.setCREATED(profile.getDsCreateDate());
            datastreamVersionType.setMIMETYPE(profile.getDsMIME());
            datastreamVersionType.setFORMATURI(profile.getDsFormatURI());

            if (this.data != null) {
                FoxmlUtils.ControlGroup controlGroup = FoxmlUtils.ControlGroup.fromExternal(profile.getDsControlGroup());
                if (controlGroup == FoxmlUtils.ControlGroup.INLINE) {
                    datastreamVersionType.setXmlContent(data.asXmlContent());
                } else if (controlGroup == FoxmlUtils.ControlGroup.MANAGED) {
//                    datastreamVersionType.setBinaryContent(IOUtils.toByteArray(this.data.asInputStream()));
                    String ref = object.getPid() + "+" + datastreamType.getID() + "+" + datastreamVersionType.getID();
                    this.manager.addOrReplaceDatastream(ref, new ByteArrayInputStream(IOUtils.toByteArray(this.data.asInputStream())));
                    datastreamVersionType.setBinaryContent(null);
                    ContentLocationType contentLocationType = new ContentLocationType();
                    contentLocationType.setTYPE("INTERNAL_ID");
                    contentLocationType.setREF(ref);
                    datastreamVersionType.setContentLocation(contentLocationType);
                } else if (controlGroup == FoxmlUtils.ControlGroup.EXTERNAL) {
                    ContentLocationType contentLocation = new ContentLocationType();
                    datastreamVersionType.setContentLocation(contentLocation);
                    contentLocation.setREF(this.data.reference.toASCIIString());
                    contentLocation.setTYPE("URL");
                } else {
                    throw new UnsupportedOperationException("DsControlGroup: " + controlGroup + "; " + toLogString());
                }
            }
            return datastreamType;
        }


        private void purgeDatastream(DatastreamProfile profile) throws IOException, StorageException {
            manager.deleteStream(this.object.getPid(), profile.getDsID());
        }

        private void fetchProfile(String versionId) throws StorageException {
            if (profile != null || missingDataStream) {
                return;
            }
            try {
                DigitalObject digitalObject = this.manager.readObjectFromStorage(this.object.getPid());
                if (object != null) {
                    this.lastModified = FoxmlUtils.getLastModified(digitalObject, dsId, versionId);
                    this.profile = FoxmlUtils.getDatastreamProfile(digitalObject, dsId, versionId);
                    this.missingDataStream = false;
                } else {
                    lastModified = -1;
                    if (defaultProfile != null) {
                        profile = defaultProfile;
                    } else {
                        throw new StorageException("Missing defialt profile for " + this.object.getPid() + "!");
                    }
                }
            } catch (IOException ex) {
                if (FoxmlUtils.missingDatastream(ex)) {
                    lastModified = -1;
                    this.missingDataStream = true;
                    if (defaultProfile != null) {
                        profile = defaultProfile;
                    } else {
                        throw new StorageException("Missing defialt profile for " + this.object.getPid() + "!");
                    }
                } else {
                    throw new StorageException(object.getPid(), toLogString(), ex);
                }
            }
        }

        private void write(DatastreamContent data, long timestamp, String message, String versionId) throws AltoEditorException {
            if (timestamp != getLastModified(versionId)) {
                String msg = String.format("%s, timestamp: %s (%s)", toLogString(), timestamp,
                        DateUtility.getXSDDateTime(new Date(timestamp)));
                throw new DigitalObjectConcurrentModificationException(object.getPid(), msg);
            }
            this.data = data;
            this.logMessage = message;
            object.register(this);
            modified = true;
            this.versionId = versionId;
        }

        private void fetchData(String versionId) throws StorageException {
            try {
                DigitalObject object = manager.readObjectFromStorage(this.object.getPid());
                if (object == null) {
                    throw new StorageException(this.object.getPid(), "Object not found in Low-Level Storage.");
                }
                for (DatastreamType datastreamType : object.getDatastream()) {
                    if (dsId.equals(datastreamType.getID())) {
                        for (DatastreamVersionType datastreamVersionType : datastreamType.getDatastreamVersion()) {
                            if (versionId.equals(datastreamVersionType.getID())) {
                                if (datastreamVersionType.getXmlContent() != null && datastreamVersionType.getXmlContent().getAny() != null && !datastreamVersionType.getXmlContent().getAny().isEmpty()) {
                                    Element node = datastreamVersionType.getXmlContent().getAny().get(0);
                                    if (node != null) {
                                        LOG.fine("Created note from xmlContent");
                                        Source xmlSource = new DOMSource(node);
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        Result outputTarget = new StreamResult(outputStream);
                                        TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
                                        this.data = new DatastreamContent(outputStream.toByteArray());
                                        break;
                                    }
                                } else if (datastreamVersionType.getBinaryContent() != null) {
                                    byte[] binaryContent = datastreamVersionType.getBinaryContent();
                                    if (binaryContent != null) {
                                        this.data = new DatastreamContent(binaryContent);
                                        break;
                                    }
                                } else if (datastreamVersionType.getContentLocation() != null) {
                                    ContentLocationType contentLocationType = datastreamVersionType.getContentLocation();
                                    if (contentLocationType != null) {
                                        String ref = contentLocationType.getREF();
                                        if (ref != null) {

                                            InputStream inputStream = this.manager.retrieveDatastream(ref);
                                            if (inputStream != null) {
                                                this.data = new DatastreamContent(IOUtils.toByteArray(inputStream));
                                                inputStream.close();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new StorageException(object.getPid(), e);
            }
        }

        private static final class DatastreamContent {

            private byte[] bytes;
            private URI reference;
            private Element xmlElement;

            public DatastreamContent(byte[] bytes) {
                this.bytes = bytes;
            }

            public DatastreamContent(URI reference) {
                this.reference = reference;
            }

            public DatastreamContent(EditorDomResult data) {
                Node root = data.getNode();
                Document doc = root.getOwnerDocument() == null ? (Document) root : root.getOwnerDocument();
                this.xmlElement = doc.getDocumentElement();
            }

            public Source asSource() {
                if (bytes != null) {
                    return new StreamSource(new ByteArrayInputStream(bytes));
                } else if (reference != null) {
                    return new StreamSource(reference.toASCIIString());
                } else {
                    return null;
                }
            }

            public InputStream asInputStream() throws IOException {
                if (bytes != null) {
                    return new ByteArrayInputStream(bytes);
                } else if (reference != null) {
                    return reference.toURL().openStream();
                } else {
                    return null;
                }
            }

            public XmlContentType asXmlContent() throws ParserConfigurationException, IOException, SAXException {
                if (xmlElement != null) {
                    XmlContentType xmlContentType = new XmlContentType();
                    xmlContentType.getAny().add(xmlElement);
                    return xmlContentType;
                }
                return null;
            }
        }

        private static final class EditorStreamResult extends StreamResult implements XmlStreamEditor.EditorResult {

            public EditorStreamResult() {
                super(new ByteArrayOutputStream());
            }

            public byte[] asBytes() {
                return ((ByteArrayOutputStream) getOutputStream()).toByteArray();
            }

        }

        private static final class EditorDomResult extends DOMResult implements EditorResult {

        }
    }
}
