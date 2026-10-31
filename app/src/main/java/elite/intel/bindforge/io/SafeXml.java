package elite.intel.bindforge.io;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The one XML parser setup BindForge reads its files with - the binds files, {@code DeviceMappings.xml} and the
 * {@code .buttonMap}s (Krondor's branch review, 2026-10-31: the same setup had been written six times).
 * <p>
 * WHY hardened: these files come from a game folder rather than the network, but a {@code .binds} or a
 * {@code DeviceMappings.xml} is also something a user can be talked into pasting in from a forum, and turning off what
 * we never use costs nothing. None of them has a DOCTYPE, so one is refused, and no entity is expanded.
 */
public final class SafeXml {

    private SafeXml() {
    }

    /**
     * A new builder, refusing a DOCTYPE, with secure processing on and entity references not expanded. It keeps the
     * default error handler; a caller that reads files that may not be XML sets its own.
     *
     * @throws ParserConfigurationException if the platform's parser does not support these settings
     */
    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }
}
