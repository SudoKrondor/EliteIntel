package elite.intel.bindforge.devicefiles;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Reading and writing the XML of the game's device files, {@code DeviceMappings.xml} and {@code .buttonMap}.
 * <p>
 * <strong>The layout is ours to choose; the content is not.</strong> The game parses these files as XML and
 * ignores whitespace, line breaks and the declaration's spacing (measured 2026-10-03, domain doc §1.2c), so
 * they are written in one readable layout: CRLF line endings, as Frontier ships them, and four spaces per
 * level. What is written is every node the document holds - comments and elements nobody has documented
 * included. Only whitespace between elements is replaced, because that is the layout.
 */
final class DeviceFileXml {

    private static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String CRLF = "\r\n";
    private static final String INDENT = "    ";

    private DeviceFileXml() {
    }

    /**
     * @throws IOException if the bytes are not well-formed XML
     */
    static Document parse(byte[] xml) throws IOException {
        try {
            return newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Not well-formed XML: " + e.getMessage(), e);
        }
    }

    static Document newDocument() {
        try {
            return newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("The platform XML parser cannot be configured", e);
        }
    }

    /**
     * Creates an element, refusing a name XML cannot hold.
     * <p>
     * A device name or input token becomes an element tag, and one with a space or a leading digit has no
     * valid tag. That is reported as a file that cannot be written, rather than thrown past the caller,
     * because it stops one write and nothing else.
     */
    static Element element(Document document, String name) throws IOException {
        try {
            return document.createElement(name);
        } catch (DOMException e) {
            throw new IOException("\"" + name + "\" cannot be written as an XML element name", e);
        }
    }

    // WHY: external entities are switched off. These files come from a game folder rather than the network,
    // but a DeviceMappings.xml is also something a user can be talked into pasting in from a forum, and
    // turning off what we never use costs nothing.
    static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    /** The document in the house layout, as UTF-8. */
    static byte[] write(Document document) {
        StringBuilder out = new StringBuilder(DECLARATION).append(CRLF);
        NodeList topLevel = document.getChildNodes();
        for (int i = 0; i < topLevel.getLength(); i++) {
            writeNode(topLevel.item(i), 0, out);
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void writeNode(Node node, int depth, StringBuilder out) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> writeElement((Element) node, depth, out);
            case Node.COMMENT_NODE -> out.repeat(INDENT, depth)
                    .append("<!--").append(node.getNodeValue()).append("-->").append(CRLF);
            case Node.PROCESSING_INSTRUCTION_NODE -> out.repeat(INDENT, depth).append(inline(node)).append(CRLF);
            // WHY: only whitespace text reaches here - writeElement keeps any text with content inline - and
            // whitespace between elements is the layout this class replaces.
            default -> {
            }
        }
    }

    /**
     * An element holding only text stays on one line, as {@code <PID>0259</PID>}; one holding elements or
     * comments gets one line per child. An element mixing real text with elements is written exactly as it
     * stands on a single line, because re-indenting it would add whitespace to its text.
     */
    private static void writeElement(Element element, int depth, StringBuilder out) {
        String indent = INDENT.repeat(depth);
        if (!hasStructuredChildren(element) || hasMeaningfulText(element)) {
            out.append(indent).append(inline(element)).append(CRLF);
            return;
        }
        out.append(indent).append(openTag(element)).append(CRLF);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            writeNode(children.item(i), depth + 1, out);
        }
        out.append(indent).append("</").append(element.getTagName()).append('>').append(CRLF);
    }

    private static boolean hasStructuredChildren(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            short type = children.item(i).getNodeType();
            if (type == Node.ELEMENT_NODE || type == Node.COMMENT_NODE
                    || type == Node.PROCESSING_INSTRUCTION_NODE) return true;
        }
        return false;
    }

    private static boolean hasMeaningfulText(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (isText(child) && !child.getNodeValue().isBlank()) return true;
        }
        return false;
    }

    /** The node and everything under it on one line, every character of its text kept. */
    private static String inline(Node node) {
        return switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> inlineElement((Element) node);
            case Node.TEXT_NODE -> escapeText(node.getNodeValue());
            case Node.CDATA_SECTION_NODE -> "<![CDATA[" + node.getNodeValue() + "]]>";
            case Node.COMMENT_NODE -> "<!--" + node.getNodeValue() + "-->";
            case Node.PROCESSING_INSTRUCTION_NODE -> "<?" + node.getNodeName() + " " + node.getNodeValue() + "?>";
            default -> throw new IllegalStateException("Unexpected XML node type " + node.getNodeType()
                    + " (" + node.getNodeName() + ")");
        };
    }

    private static String inlineElement(Element element) {
        NodeList children = element.getChildNodes();
        if (children.getLength() == 0) return openTag(element).replaceFirst(">$", "/>");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            body.append(inline(children.item(i)));
        }
        return openTag(element) + body + "</" + element.getTagName() + ">";
    }

    private static String openTag(Element element) {
        StringBuilder tag = new StringBuilder("<").append(element.getTagName());
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            tag.append(' ').append(attribute.getName()).append("=\"")
                    .append(escapeAttribute(attribute.getValue())).append('"');
        }
        return tag.append('>').toString();
    }

    private static boolean isText(Node node) {
        return node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE;
    }

    private static String escapeText(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;");
    }
}
