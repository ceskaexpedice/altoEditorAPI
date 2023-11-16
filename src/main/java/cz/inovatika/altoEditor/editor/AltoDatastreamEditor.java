/*
 * Copyright (C) 2014 Jan Pokorsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

/**
 * ALTO data stream.
 *
 * @author Lukas Sykora
 */
public final class AltoDatastreamEditor {

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
        try {
            if (!isAlto(alto)) {
                throw new DigitalObjectException(dObj.getPid(),
                        String.format("%s: missing expected ALTO version: %s",
                                ALTO_ID, AltoDatastreamEditor.ALTO_FORMAT_URI),
                        null);
            }
        } catch (Exception ex) {
            throw new DigitalObjectException(dObj.getPid(), ALTO_ID, ex);
        }
        XmlStreamEditor editor = dObj.getEditor(altoProfile());
        editor.write(alto.getBytes(StandardCharsets.UTF_8), editor.getLastModified(versionId), msg, versionId);
        dObj.flush();
    }

    /**
     * Checks whether URI content contains proper ALTO data.
     * @param alto URI
     * @throws IOException failure
     */
    static boolean isAlto(URI alto) throws IOException, SAXException {
        return validSchema(getSchemas(), new StreamSource(alto.toASCIIString()));
        //getSchema().newValidator().validate(new StreamSource(alto.toASCIIString()));
    }

    static boolean isAlto(String alto) throws SAXException, IOException {
        return validSchema(getSchemas(), new StreamSource(new StringReader(alto)));
    }

    private static boolean validSchema(List<Schema> schemas, StreamSource altoSource) throws IOException, SAXException {
        SAXException exception = new SAXException();

        for (Schema schema : schemas) {
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
        InputStream altoStream = editor.readStream(versionId);
        if (altoStream == null) {
            throw new AltoEditorException("Alto datastream with version " + versionId + " not found in storage");
        }
        String alto = new BufferedReader(
                new InputStreamReader(altoStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

        AltoEditorStringRecordResponse response = new AltoEditorStringRecordResponse();
        response.setData(alto);
        response.setTimestamp(editor.getLastModified(versionId));
        return response;
    }

    public static String nextVersion(String version) {
        Integer currentVersionId = getVersionId(version);
        version = AltoDatastreamEditor.ALTO_ID + "." + (currentVersionId + 1);
        return version;
    }

    public static Integer getVersionId(String version) {
        version = version.substring((AltoDatastreamEditor.ALTO_ID + ".").length());
        Integer versionId = Integer.valueOf(version);
        return versionId;
    }
}
