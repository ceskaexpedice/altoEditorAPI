package cz.inovatika.altoEditor.utils;

import java.io.InputStream;
import java.util.HashMap;

import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

public class XmlLSResolver implements LSResourceResolver {

    static final  HashMap<String, String> URL_MAP = new HashMap<String, String>();
    private static XmlLSResolver INSTANCE;

    private final DOMImplementationLS dls;

    static {
        URL_MAP.put("http://www.w3.org/2001/03/xml.xsd", "/xsd/xml.xsd");
        URL_MAP.put("http://www.loc.gov/standards/xlink/xlink.xsd", "/xsd/xlink.xsd");
        URL_MAP.put("http://www.loc.gov/mods/xml.xsd", "/xsd/xml.xsd");
    }

    public static XmlLSResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new XmlLSResolver();
        }
        return INSTANCE;
    }

    private XmlLSResolver() {
        try {
            dls = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        String location = URL_MAP.get(systemId);
        if (location == null) {
            throw new IllegalStateException("Unable to find mapping for:" + systemId);
        }
        InputStream is = this.getClass().getResourceAsStream(location);
        LSInput input = dls.createLSInput();
        input.setByteStream(is);
        input.setPublicId(publicId);
        input.setSystemId(systemId);
        return input;
    }

}
