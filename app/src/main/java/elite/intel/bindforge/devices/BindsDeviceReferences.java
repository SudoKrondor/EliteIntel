package elite.intel.bindforge.devices;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Which device entries the bindings actually name.
 * <p>
 * This is what separates a divergence that breaks something from one that is merely untidy: an entry missing
 * from an installation only costs the user anything when a binding names that device.
 * <p>
 * Read from <strong>every</strong> {@code .binds} in the folder rather than the active preset alone, because
 * the game validates all of them - measured 2026-09-22, where one unresolvable name in any file had the whole
 * preset rejected.
 */
public final class BindsDeviceReferences {

    private static final String BINDS_SUFFIX = ".binds";
    private static final String DEVICE_ATTRIBUTE = "Device";

    /**
     * Values that appear in a {@code Device=} attribute but are not device entries. Keyboard and mouse are
     * named directly and have no entry in {@code DeviceMappings.xml}; {@code {NoDevice}} means the slot is
     * empty.
     */
    private static final Set<String> NOT_AN_ENTRY = Set.of("keyboard", "mouse", "{nodevice}", "");

    private BindsDeviceReferences() {
    }

    /**
     * The entry names referenced by the {@code .binds} files in {@code bindingsFolder}.
     * <p>
     * A file that cannot be parsed is skipped rather than failing the scan - the folder demonstrably holds
     * files with a {@code .binds} name that are not binds files, most often a {@code StartPreset} picked by
     * mistake, and one of those must not stop the rest being read.
     */
    public static Set<String> referencedEntryNames(Path bindingsFolder) throws IOException {
        Set<String> names = new LinkedHashSet<>();
        for (Path file : bindsFilesIn(bindingsFolder)) {
            try {
                collectInto(file, names);
            } catch (SAXException | ParserConfigurationException | IOException e) {
                // WHY: skipped, not fatal. See above - one unreadable file in the folder is an ordinary
                // condition, and the names in the others are still worth having.
                continue;
            }
        }
        return Set.copyOf(names);
    }

    private static void collectInto(Path bindsFile, Set<String> names)
            throws IOException, SAXException, ParserConfigurationException {

        NodeList all = newDocumentBuilder().parse(bindsFile.toFile()).getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (!element.hasAttribute(DEVICE_ATTRIBUTE)) continue;
            String device = element.getAttribute(DEVICE_ATTRIBUTE).trim();
            if (isAnEntryName(device)) names.add(device);
        }
    }

    /**
     * Whether this {@code Device=} value names an entry in {@code DeviceMappings.xml}.
     * <p>
     * A {@code .binds} names a device by its element name when an entry matches it, and <strong>falls back to
     * raw VID+PID hex when none does</strong> - eight hex characters, vendor then product. Those are not entry
     * names: nothing in {@code DeviceMappings.xml} is called {@code 334403F3}, and a binding using one is
     * telling us the device has no entry at all, which is a different problem from a divergent one.
     */
    private static boolean isAnEntryName(String device) {
        if (NOT_AN_ENTRY.contains(device.toLowerCase(Locale.ROOT))) return false;
        return !isRawHardwareId(device);
    }

    private static boolean isRawHardwareId(String device) {
        if (device.length() != 8) return false;
        for (int i = 0; i < device.length(); i++) {
            if (Character.digit(device.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    private static List<Path> bindsFilesIn(Path bindingsFolder) throws IOException {
        if (!Files.isDirectory(bindingsFolder)) return List.of();
        try (Stream<Path> files = Files.list(bindingsFolder)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(BINDS_SUFFIX))
                    .sorted()
                    .toList();
        }
    }

    // WHY: the same hardening as DeviceMappingsParser, and for the same reason - these files are shared
    // between players often enough that turning off what we never use costs nothing.
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }
}
