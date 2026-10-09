package elite.intel.bindforge.devicefiles;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
            Document document = DeviceFileXml.newDocumentBuilder().parse(deviceMappings.toFile());
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

    /**
     * The same, from an open stream - which is how Frontier's shipped reference copy is read, since it
     * travels inside the jar rather than sitting on disk.
     *
     * @param what names the source in an error, a resource path having no useful {@code toString}
     */
    public static List<DeviceEntry> parse(InputStream xml, String what) throws IOException {
        try {
            return entriesOf(DeviceFileXml.newDocumentBuilder().parse(xml));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Could not read " + what + ": " + e.getMessage(), e);
        }
    }

    /**
     * Each element's own {@code <VID>} and {@code <PID>} - its primary pair, alternatives left out - by element
     * name, in document order.
     * <p>
     * {@link DeviceEntry} holds an element's pairs as a set, so which one is primary is lost there. The primary
     * pair is the one Apply writes and the one the master holds, so comparing a file against the master needs it.
     * An element named twice keeps its first, because the game resolves to the first match.
     *
     * @return an element with neither child maps to an empty pair rather than being left out, since the element
     *         itself is still there
     * @throws IOException if the bytes are not well-formed XML
     */
    public static Map<String, DeviceEntry.HardwareId> primaries(byte[] xml) throws IOException {
        Map<String, DeviceEntry.HardwareId> primaries = new LinkedHashMap<>();
        Element root = DeviceFileXml.parse(xml).getDocumentElement();
        if (root == null) return primaries;

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element element)) continue;
            primaries.putIfAbsent(element.getTagName(), new DeviceEntry.HardwareId(
                    directChildText(element, VID), directChildText(element, PID)));
        }
        return primaries;
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

    /** One element as an entry: its tag, and every pair it claims, primary and {@code <Alternative>}. */
    static DeviceEntry entryOf(Element element) {
        Set<DeviceEntry.HardwareId> hardware = new LinkedHashSet<>();
        addHardwareOf(element, hardware);

        NodeList alternatives = element.getElementsByTagName(ALTERNATIVE);
        for (int i = 0; i < alternatives.getLength(); i++) {
            addHardwareOf((Element) alternatives.item(i), hardware);
        }
        // WHY: kept in file order, primary first, rather than through Set.copyOf - whose iteration order is
        // hash-derived and salted per run. The primary pair is the one shown for an entry, and the one recorded
        // for it; with Set.copyOf a multi-pair entry such as <GamePad> gave a different "first" pair every run.
        return new DeviceEntry(element.getTagName(), Collections.unmodifiableSet(hardware));
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
}
