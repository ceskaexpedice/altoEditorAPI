package cz.inovatika.altoEditor.editor;

import com.yourmediashelf.fedora.generated.management.DatastreamProfile;
import cz.inovatika.altoEditor.exception.AltoEditorException;
import cz.inovatika.altoEditor.exception.DigitalObjectException;
import cz.inovatika.altoEditor.response.AltoEditorStringRecordResponse;
import cz.inovatika.altoEditor.storage.DigitalObject;
import cz.inovatika.altoEditor.utils.FoxmlUtils;
import cz.inovatika.altoEditor.utils.XmlLSResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * ALTO data stream.
 *
 * @author Lukas Sykora
 */
public final class AltoDatastreamEditor {

    private static final Logger LOGGER = LogManager.getLogger(AltoDatastreamEditor.class.getName());

    public static final String ALTO_ID = "ALTO";
    public static final String ALTO_LABEL = "ALTO for this object";
    public static final String ALTO_FORMAT_URI = "http://www.loc.gov/standards/alto/ns-v2#";

    private static final String ALTO_SCHEMA_PATH_20 = "/xsd/alto-v2.0.xsd";
    private static final String ALTO_SCHEMA_PATH_21 = "/xsd/alto-v2.1.xsd";
    private static final String ALTO_SCHEMA_PATH_30 = "/xsd/alto-v3.0.xsd";

    private DigitalObject object;
    private XmlStreamEditor editor;

    public AltoDatastreamEditor(DigitalObject object, DatastreamProfile profile) {
        this(object.getEditor(profile), object);
    }

    public AltoDatastreamEditor(XmlStreamEditor editor, DigitalObject object) {
        this.object = object;
        this.editor = editor;
    }

    public static AltoDatastreamEditor alto(DigitalObject object) {
        return new AltoDatastreamEditor(object, altoProfile());
    }

    public static DatastreamProfile altoProfile() {
        return FoxmlUtils.managedVersionableProfile(ALTO_ID, ALTO_FORMAT_URI, ALTO_LABEL);
    }

    /**
     * Adds ALTO content to a digital object
     * @param dObj digital object
     * @param altoUri OCR
     * @param msg log message
     * @throws cz.inovatika.altoEditor.exception.AltoEditorException failure
     */
    public static void importAlto(DigitalObject dObj, URI altoUri, String msg, String versionId) throws AltoEditorException {
        try {
            if (!isAlto(altoUri)) {
                throw new DigitalObjectException(dObj.getPid(),
                        String.format("%s: missing expected ALTO version: %s",
                                altoUri.toASCIIString(), AltoDatastreamEditor.ALTO_FORMAT_URI),
                        null);
            }
        } catch (Exception ex) {
            throw new DigitalObjectException(dObj.getPid(), altoUri.toASCIIString(), ex);
        }
        XmlStreamEditor editor = dObj.getEditor(altoProfile());
        editor.write(altoUri, editor.getLastModified(versionId), msg, versionId);
    }

    public static void updateAlto(DigitalObject dObj, String alto, String msg, String versionId) throws AltoEditorException {
        alto = fixAlto(alto);
        alto = indentXml(dObj.getPid(), alto);
        try {
            if (!isAlto(alto)) {
                throw new DigitalObjectException(dObj.getPid(),
                        String.format("%s: missing expected ALTO version: %s",
                                ALTO_ID, AltoDatastreamEditor.ALTO_FORMAT_URI),
                        null);
            }
        } catch (Exception ex) {
            LOGGER.debug(alto);
            throw new DigitalObjectException(dObj.getPid(), ALTO_ID + ": " + ex.getMessage(), ex);
        }
        XmlStreamEditor editor = dObj.getEditor(altoProfile());
        editor.write(alto.getBytes(StandardCharsets.UTF_8), editor.getLastModified(versionId), msg, versionId);
        dObj.flush();
    }

    private static String indentXml(String pid, String altoContent) throws AltoEditorException {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(altoContent)));
            OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(2);
            format.setOmitXMLDeclaration(true);
            format.setLineWidth(Integer.MAX_VALUE);
            Writer outxml = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(outxml, format);
            serializer.serialize(doc);
            return outxml.toString();
        } catch (ParserConfigurationException | IOException  | SAXException ex) {
            ex.printStackTrace();
            LOGGER.error("PID: " + pid + " - " + ex.getMessage());
            throw new AltoEditorException(pid, ex);
        }
    }

    private static String fixAlto(String alto) {
        if (alto.startsWith("<root>")) {
            alto = alto.replace("<root>", "<?xml version=\"1.0\" encoding=\"utf-8\"?><alto xmlns=\"http://www.loc.gov/standards/alto/ns-v2#\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/alto/ns-v2# http://www.loc.gov/standards/alto/v2/alto-2-0.xsd\">");
        }
        if (alto.endsWith("</root>")) {
            alto = alto.replace("</root>", "</alto>");
        }
        return alto;
    }

    /**
     * Checks whether URI content contains proper ALTO data.
     * @param alto URI
     * @throws IOException failure
     */
    static boolean isAlto(URI alto) throws IOException, SAXException {
        SAXException exception = new SAXException();
        List<Schema> schemas = getSchemas();
        for (Schema schema : schemas) {
            StreamSource altoSource = new StreamSource(alto.toASCIIString());
            try {
                schema.newValidator().validate(altoSource);
                return true;
            } catch (SAXException ex) {
                exception = ex;
            }
        }
        if (!exception.getMessage().isEmpty()) {
            throw exception;
        }
        return false;
    }

    static boolean isAlto(String alto) throws SAXException, IOException {
        SAXException exception = new SAXException();
        List<Schema> schemas = getSchemas();
        for (Schema schema : schemas) {
            StreamSource altoSource = new StreamSource(new StringReader(alto));
            try {
                schema.newValidator().validate(altoSource);
                return true;
            } catch (SAXException ex) {
                exception = ex;
            }
        }
        if (!exception.getMessage().isEmpty()) {
            throw exception;
        }
        return false;
    }

    public static List<Schema> getSchemas() throws SAXException {
        List <Schema> schemas = new ArrayList<>();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setResourceResolver(XmlLSResolver.getInstance());
        schemas.add(schemaFactory.newSchema(AltoDatastreamEditor.class.getResource(ALTO_SCHEMA_PATH_20)));
        schemas.add(schemaFactory.newSchema(AltoDatastreamEditor.class.getResource(ALTO_SCHEMA_PATH_21)));
        schemas.add(schemaFactory.newSchema(AltoDatastreamEditor.class.getResource(ALTO_SCHEMA_PATH_30)));
        return schemas;
    }

    public AltoEditorStringRecordResponse readRecord(@NotNull String versionId) throws AltoEditorException {
        String alto = readRecordAsString(versionId);

        AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
        response.setData(alto);
        response.setVersion(versionId);
        response.setTimestamp(editor.getLastModified(versionId));
        return response;
    }

    public String readRecordAsString(String versionId) throws AltoEditorException {
        InputStream altoStream = editor.readStream(versionId);
        if (altoStream == null) {
            throw new AltoEditorException("Alto datastream with version " + versionId + " not found in storage");
        }
        String alto = new BufferedReader(
                new InputStreamReader(altoStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        return alto;
    }

    public static String nextVersion(String version) {
        Integer currentVersionId = getVersionId(version);
        if (currentVersionId > 2) {
            version = AltoDatastreamEditor.ALTO_ID + "." + (currentVersionId + 1);
        } else {
            version = AltoDatastreamEditor.ALTO_ID + ".2";
        }
        return version;
    }

    public static Integer getVersionId(String version) {
        version = version.substring((AltoDatastreamEditor.ALTO_ID + ".").length());
        Integer versionId = Integer.valueOf(version);
        return versionId;
    }
}
