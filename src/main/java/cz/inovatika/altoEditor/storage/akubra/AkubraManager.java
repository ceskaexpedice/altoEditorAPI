package cz.inovatika.altoEditor.storage.akubra;

import com.yourmediashelf.fedora.generated.foxml.ContentLocationType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamVersionType;
import com.yourmediashelf.fedora.generated.foxml.DigitalObject;
import com.yourmediashelf.fedora.generated.foxml.PropertyType;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.StorageException;
import cz.inovatika.altoEditor.utils.Config;
import cz.inovatika.altoEditor.utils.Const;
import cz.inovatika.altoEditor.utils.FoxmlUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.akubraproject.BlobStore;
import org.akubraproject.fs.FSBlobStore;
import org.akubraproject.map.IdMapper;
import org.akubraproject.map.IdMappingBlobStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.ICheckable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.lowlevel.akubra.AkubraLowlevelStorage;
import org.fcrepo.server.storage.lowlevel.akubra.HashPathIdMapper;

import static cz.inovatika.altoEditor.utils.FoxmlUtils.createXmlDate;

public class AkubraManager {

    private static final Logger LOGGER = LogManager.getLogger(AkubraManager.class.getName());
    private ILowlevelStorage storage;

    private static Unmarshaller unmarshaller = null;
    private static Marshaller marshaller = null;

    static {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DigitalObject.class);
            unmarshaller = jaxbContext.createUnmarshaller();

            //JAXBContext jaxbdatastreamContext = JAXBContext.newInstance(DatastreamType.class);
            marshaller = jaxbContext.createMarshaller();

        } catch (Exception e) {
            LOGGER.error("Cannot init JAXB", e);
            throw new RuntimeException(e);
        }
    }

    public AkubraManager() throws IOException {
        try {
            this.storage = createAkubraLowLevelStorage();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private AkubraLowlevelStorage createAkubraLowLevelStorage() throws Exception {
        BlobStore fsObjectStore = new FSBlobStore(new URI("urn:example.org:fsObjectStore"), new File(Config.getObjectStorePath()));
        IdMapper fsObjectStoreMapper = new HashPathIdMapper(Config.getObjectStorePattern());
        BlobStore objectStore = new IdMappingBlobStore(new URI("urn:example.org:objectStore"), fsObjectStore, fsObjectStoreMapper);
        BlobStore fsDatastreamStore = new FSBlobStore(new URI("urn:example.org:fsDatastreamStore"), new File(Config.getDataStreamStorePath()));
        IdMapper fsDatastreamStoreMapper = new HashPathIdMapper(Config.getDataStreamStorePattern());
        BlobStore datastreamStore = new IdMappingBlobStore(new URI("urn:example.org:datastreamStore"), fsDatastreamStore, fsDatastreamStoreMapper);
        AkubraLowlevelStorage retval = new AkubraLowlevelStorage(objectStore, datastreamStore, true, true);
        return retval;
    }

    public boolean objectExists(String pid) throws StorageException {
        return readObjectFromStorage(pid) != null;
    }

    public DigitalObject readObjectFromStorage(String pid) throws StorageException {
        Object obj;
        try (InputStream inputStream = this.storage.retrieveObject(pid);){
            synchronized (unmarshaller) {
                obj = unmarshaller.unmarshal(inputStream);
            }
        } catch (ObjectNotInLowlevelStorageException ex) {
            return null;
        } catch (Exception e) {
            throw new StorageException(pid, e);
        }
        return (DigitalObject) obj;
    }

    public InputStream retrieveDatastream(String dsKey) throws IOException {
        try {
            return storage.retrieveDatastream(dsKey);
        } catch (LowlevelStorageException e) {
            throw new IOException(e);
        }
    }

    public InputStream retrieveObject(String objectKey) throws IOException {
        try {
            return storage.retrieveObject(objectKey);
        } catch (LowlevelStorageException e) {
            throw new IOException(e);
        }
    }

    public void deleteObject(String pid, boolean includingManagedDatastreams) throws IOException, StorageException {
        DigitalObject object = readObjectFromStorage(pid);
        if (includingManagedDatastreams) {
            for (DatastreamType datastreamType : object.getDatastream()) {
                removeManagedStream(datastreamType);
            }
        }
        try {
            storage.removeObject(pid);
        } catch (LowlevelStorageException e) {
            LOGGER.warn("Could not remove object from Akubra: " + e);
        }
    }

    public void deleteStream(String pid, String streamId) throws IOException, StorageException {
        DigitalObject object = readObjectFromStorage(pid);
        List<DatastreamType> datastreamList = object.getDatastream();
        Iterator<DatastreamType> iterator = datastreamList.iterator();
        while (iterator.hasNext()) {
            DatastreamType datastreamType = iterator.next();
            if (streamId.equals(datastreamType.getID())) {
                removeManagedStream(datastreamType);
                iterator.remove();
                break;
            }
        }
        try {
            FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
            StringWriter stringWriter = new StringWriter();
            synchronized (marshaller) {
                marshaller.marshal(object, stringWriter);
            }
            addOrReplaceObject(pid, new ByteArrayInputStream(stringWriter.toString().getBytes("UTF-8")));
        } catch (Exception e) {
            LOGGER.warn("Could not replace object in Akubra: " + e);
        }
    }

    private void removeManagedStream(DatastreamType datastreamType) {
        if ("M".equals(datastreamType.getCONTROLGROUP())) {
            for (DatastreamVersionType datastreamVersionType : datastreamType.getDatastreamVersion()) {
                if ("INTERNAL_ID".equals(datastreamVersionType.getContentLocation().getTYPE())) {
                    try {
                        storage.removeDatastream(datastreamVersionType.getContentLocation().getREF());
                    } catch (LowlevelStorageException e) {
                        LOGGER.warn("Could not remove managed datastream from Akubra: " + e);
                    }
                }
            }
        }
    }

    public void commit(DigitalObject object, String streamId) throws AltoEditorException {
        List<DatastreamType> datastreamList = object.getDatastream();
        Iterator<DatastreamType> iterator = datastreamList.iterator();
        while (iterator.hasNext()) {
            DatastreamType datastream = iterator.next();
            ensureDsVersionCreatedDate(object.getPID(), datastream);
            if (streamId != null && streamId.equals(datastream.getID())) {
                convertManagedStream(object.getPID(), datastream);
                break;
            } else {
                convertManagedStream(object.getPID(), datastream);
            }
        }
        try {
            FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_LASTMODIFIED, FoxmlUtils.getActualDateAsString());
            FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_STATE, Const.FOXML_PROPERTY_STATE_ACTIVE);
            PropertyType createdProperty = FoxmlUtils.findProperty(object, Const.FOXML_PROPERTY_CREATEDATE);
            if (createdProperty == null) {
                FoxmlUtils.setProperty(object, Const.FOXML_PROPERTY_CREATEDATE, FoxmlUtils.getActualDateAsString());
            }
            StringWriter stringWriter = new StringWriter();
            synchronized (marshaller) {
                marshaller.marshal(object, stringWriter);
            }
            addOrReplaceObject(object.getPID(), new ByteArrayInputStream(stringWriter.toString().getBytes("UTF-8")));
        } catch (Exception e) {
            LOGGER.warn("Could not replace object in Akubra: " + e);
        }
    }

    public InputStream marshallObject(DigitalObject object) {
        try {
            StringWriter stringWriter = new StringWriter();
            synchronized (marshaller) {
                marshaller.marshal(object, stringWriter);
            }
            return new ByteArrayInputStream(stringWriter.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            LOGGER.warn("Could not marshall object: " + e);
            throw new RuntimeException(e);
        }
    }

    private void ensureDsVersionCreatedDate(String pid, DatastreamType datastream) throws StorageException {
        if (datastream != null) {
            for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
                XMLGregorianCalendar created = datastreamVersion.getCREATED();
                if (created == null) {
                    try {
                        datastreamVersion.setCREATED(createXmlDate());
                    } catch (DatatypeConfigurationException ex) {
                        throw new StorageException(pid, ex);
                    }
                }
            }
        }
    }

    private void convertManagedStream(String pid, DatastreamType datastream) {
        if ("M".equals(datastream.getCONTROLGROUP())) {
            for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
                if (datastreamVersion.getBinaryContent() != null) {
                    try {
                        String ref = pid + "+" + datastream.getID() + "+" + datastreamVersion.getID();
                        addOrReplaceDatastream(ref, new ByteArrayInputStream(datastreamVersion.getBinaryContent()));
                        datastreamVersion.setBinaryContent(null);
                        ContentLocationType contentLocationType = new ContentLocationType();
                        contentLocationType.setTYPE("INTERNAL_ID");
                        contentLocationType.setREF(ref);
                        datastreamVersion.setContentLocation(contentLocationType);
                    } catch (LowlevelStorageException e) {
                        LOGGER.warn("Could not remove managed datastream from Akubra: " + e);
                    }
                }
            }
        }
    }

    public void addOrReplaceObject(String pid, InputStream content) throws LowlevelStorageException {
        if (((ICheckable) storage).objectExists(pid)) {
            storage.replaceObject(pid, content, null);
        } else {
            storage.addObject(pid, content, null);
        }
    }

    public void addOrReplaceDatastream(String pid, InputStream content) throws LowlevelStorageException {
        if (storage instanceof AkubraLowlevelStorage) {
            if (((AkubraLowlevelStorage) storage).datastreamExists(pid)) {
                storage.replaceDatastream(pid, content, null);
            } else {
                storage.addDatastream(pid, content, null);
            }
        } else {
            try {
                storage.addDatastream(pid, content, null);
            } catch (ObjectAlreadyInLowlevelStorageException oailse) {
                storage.replaceDatastream(pid, content, null);
            }
        }
    }

}
