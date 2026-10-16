package elite.intel.bindforge.devicefiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final Logger log = LogManager.getLogger(BindsDeviceReferences.class);

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
     * <p>
     * Lenient: a folder that is not there names nothing. That suits severity, where it only turns findings yellow;
     * anything deciding a removal uses {@link #referenceCounts(Path)}, which refuses to guess.
     */
    public static Set<String> referencedEntryNames(Path bindingsFolder) throws IOException {
        if (!Files.isDirectory(bindingsFolder)) return Set.of();
        return Set.copyOf(read(bindingsFolder).counts().keySet());
    }

    /**
     * How many bindings name each entry, across every {@code .binds} in {@code bindingsFolder} - one per
     * {@code Device=} naming it. The count is what makes a question about the device answerable: 142 bindings is
     * worth a moment's thought, 2 is not.
     * <p>
     * <strong>Strict, because it gates removals</strong> - whether CLEAR is offered, and whether Apply takes an entry
     * out (review, 2026-10-11). A folder that is not there, or one whose {@code .binds} files all fail to parse, would
     * otherwise read as "nothing names this device", and removing a named entry makes the game reject the whole
     * preset. So both throw, and the callers take their "cannot read" path. One unparsable file among readable ones
     * is still skipped, as above. An empty folder is an answer: no custom preset names anything.
     *
     * @throws IOException when the folder is missing or no {@code .binds} in it could be read
     */
    public static Map<String, Integer> referenceCounts(Path bindingsFolder) throws IOException {
        return readStrictly(bindingsFolder).counts();
    }

    /**
     * How many bindings name each device by its hardware id - eight hex characters, the game's fallback for a device
     * with no entry - across every {@code .binds} in {@code bindingsFolder}, keyed by the value exactly as found
     * (Alan, 2026-10-14, A6b). Onboarding counts these to say how many bindings a name will move, and Apply rewrites
     * them <em>from the value found</em> (Krondor, 2026-10-13), so a reference written in either case or order is
     * moved as it is written.
     * <p>
     * Strict, as {@link #referenceCounts(Path)} is: it decides a write.
     *
     * @throws IOException when the folder is missing or no {@code .binds} in it could be read
     */
    public static Map<String, Integer> hardwareIdCounts(Path bindingsFolder) throws IOException {
        return readStrictly(bindingsFolder).hardwareIds();
    }

    /**
     * The hardware ids among {@code counts} that name this VID and PID, with their counts. Read in either order - VID
     * then PID, as the game writes, or the reverse a hand-edited file may hold (format §4.2) - and ignoring case.
     */
    public static Map<String, Integer> hardwareIdsFor(String vid, String pid, Map<String, Integer> counts) {
        String forward = (vid + pid).toUpperCase(Locale.ROOT);
        String reversed = (pid + vid).toUpperCase(Locale.ROOT);
        Map<String, Integer> found = new LinkedHashMap<>();
        counts.forEach((id, count) -> {
            String upper = id.toUpperCase(Locale.ROOT);
            if (upper.equals(forward) || upper.equals(reversed)) found.put(id, count);
        });
        return found;
    }

    /**
     * How many bindings name a device by any of {@code names}, ignoring case - the one count the Device Editor's CLEAR
     * and Apply's rename move both use, so they cannot disagree (review, 2026-10-15, A11). A hand-edited
     * {@code .binds} can spell a name otherwise, and missing one would let CLEAR strand it. Each binding is counted
     * once, however many of the names match it; {@code null} names are ignored.
     *
     * @param counts as {@link #referenceCounts(Path)} returns them, keyed as written
     */
    public static int bindingsNaming(Collection<String> names, Map<String, Integer> counts) {
        Set<String> folded = new HashSet<>();
        for (String name : names) {
            if (name != null) folded.add(name.toLowerCase(Locale.ROOT));
        }
        int count = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (folded.contains(entry.getKey().toLowerCase(Locale.ROOT))) count += entry.getValue();
        }
        return count;
    }

    private static Reading readStrictly(Path bindingsFolder) throws IOException {
        if (!Files.isDirectory(bindingsFolder)) {
            throw new NoSuchFileException(bindingsFolder.toString(), null, "the bindings folder is not there");
        }
        Reading reading = read(bindingsFolder);
        if (reading.files() > 0 && reading.parsed() == 0) {
            throw new IOException("none of the " + reading.files() + " .binds files in " + bindingsFolder
                    + " could be read");
        }
        return reading;
    }

    private static Reading read(Path bindingsFolder) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Integer> hardwareIds = new LinkedHashMap<>();
        List<Path> files = bindsFilesIn(bindingsFolder);
        int parsed = 0;
        for (Path file : files) {
            try {
                collectInto(file, counts, hardwareIds);
                parsed++;
            } catch (SAXException | ParserConfigurationException | IOException e) {
                // WHY: skipped, not fatal. See above - one unreadable file in the folder is an ordinary
                // condition, and the names in the others are still worth having. Logged, because these names now
                // decide whether a device may be cleared, and a support bundle should say which file was skipped.
                log.warn("Skipped {} while reading which devices the bindings name: {}", file, e.getMessage());
            }
        }
        return new Reading(Map.copyOf(counts), Map.copyOf(hardwareIds), files.size(), parsed);
    }

    private record Reading(Map<String, Integer> counts, Map<String, Integer> hardwareIds, int files, int parsed) {
    }

    private static void collectInto(Path bindsFile, Map<String, Integer> counts, Map<String, Integer> hardwareIds)
            throws IOException, SAXException, ParserConfigurationException {

        NodeList all = newDocumentBuilder().parse(bindsFile.toFile()).getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (!element.hasAttribute(DEVICE_ATTRIBUTE)) continue;
            String device = element.getAttribute(DEVICE_ATTRIBUTE).trim();
            if (isRawHardwareId(device)) {
                hardwareIds.merge(device, 1, Integer::sum);
            } else if (isAnEntryName(device)) {
                counts.merge(device, 1, Integer::sum);
            }
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
