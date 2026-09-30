package elite.intel.bindforge.devices;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads a {@code DeviceMappings.xml} into its device entries.
 * <p>
 * The file is a flat list of elements under {@code <Root>}, each named for the device it describes and
 * holding a {@code <PID>} and {@code <VID>}, optionally followed by {@code <Alternative>} blocks carrying
 * more pairs for the same entry.
 * <p>
 * <strong>Nothing in the file marks which entries Frontier shipped and which the user added</strong> - a
 * hand-added device sits indistinguishably among the 51 stock ones. Telling them apart needs the shipped
 * reference copy, and is not this parser's job.
 */
public final class DeviceMappingsParser {

    private static final String ALTERNATIVE = "Alternative";
    private static final String VID = "VID";
    private static final String PID = "PID";

    private DeviceMappingsParser() {
    }

    /**
     * Parses the file at {@code deviceMappings}.
     *
     * @return the entries in document order
     * @throws IOException if the file cannot be read or is not valid XML
     */
    public static List<DeviceEntry> parse(Path deviceMappings) throws IOException {
        try {
            Document document = newDocumentBuilder().parse(deviceMappings.toFile());
            return entriesOf(document);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Could not read " + deviceMappings + ": " + e.getMessage(), e);
        }
    }

    /**
     * The same, for a file that may not be there.
     *
     * @return the entries, or an empty list when the file does not exist - an installation with no
     *         {@code DeviceMappings.xml} is a real state, not a failure
     */
    public static List<DeviceEntry> parseIfPresent(Path deviceMappings) throws IOException {
        if (!Files.isRegularFile(deviceMappings)) return List.of();
        return parse(deviceMappings);
    }

    private static List<DeviceEntry> entriesOf(Document document) {
        List<DeviceEntry> entries = new ArrayList<>();
        Element root = document.getDocumentElement();
        if (root == null) return entries;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            entries.add(entryOf((Element) node));
        }
        return entries;
    }

    private static DeviceEntry entryOf(Element element) {
        Set<DeviceEntry.HardwareId> hardware = new LinkedHashSet<>();
        addHardwareOf(element, hardware);

        NodeList alternatives = element.getElementsByTagName(ALTERNATIVE);
        for (int i = 0; i < alternatives.getLength(); i++) {
            addHardwareOf((Element) alternatives.item(i), hardware);
        }
        return new DeviceEntry(element.getTagName(), Set.copyOf(hardware));
    }

    /**
     * Takes the VID and PID belonging to {@code element} itself, ignoring any nested in an
     * {@code <Alternative>} - those are collected separately, so reading them here would attribute an
     * alternative's ids to the primary pair as well.
     */
    private static void addHardwareOf(Element element, Set<DeviceEntry.HardwareId> hardware) {
        String vid = directChildText(element, VID);
        String pid = directChildText(element, PID);
        if (vid == null && pid == null) return;
        hardware.add(new DeviceEntry.HardwareId(vid == null ? "" : vid, pid == null ? "" : pid));
    }

    private static String directChildText(Element element, String tagName) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return node.getTextContent();
            }
        }
        return null;
    }

    // WHY: external entities are switched off. These files come from a game folder rather than the network,
    // but a DeviceMappings.xml is also something a user can be talked into pasting in from a forum, and
    // turning off what we never use costs nothing.
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }
}
