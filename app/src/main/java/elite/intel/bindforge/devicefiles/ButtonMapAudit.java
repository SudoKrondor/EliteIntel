package elite.intel.bindforge.devicefiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Checks one installation's {@code .buttonMap} files against its own device entries.
 * <p>
 * A {@code .buttonMap} is named for the element tag it belongs to - {@code VPCPanel.buttonMap} carries the
 * labels for {@code <VPCPanel>} - so the filename is the whole of the link between them. A file whose name
 * matches no entry resolves to nothing: the game never reads it and the labels inside it are never shown.
 * <p>
 * This is a check <strong>within</strong> an installation, not between installations, which is why it stays
 * useful after every installation has been standardised onto the same files.
 */
public final class ButtonMapAudit {

    private static final String SUFFIX = ".buttonMap";

    private ButtonMapAudit() {
    }

    /**
     * @param attached          files whose name matches a device entry, and the entry they belong to
     * @param orphaned          files matching no entry - nothing resolves through them
     * @param entriesWithoutMap entries with no file of their own, which is <strong>the normal case</strong>
     *                          rather than a fault: Frontier ships button maps for only a couple of devices
     */
    public record Result(List<Attachment> attached, List<Path> orphaned, List<String> entriesWithoutMap) {

        /** True when every file in the folder belongs to an entry. */
        public boolean isClean() {
            return orphaned.isEmpty();
        }
    }

    /**
     * @param deviceName the entry the file belongs to, spelled as the entry spells it
     * @param file       the file itself
     */
    public record Attachment(String deviceName, Path file) {
    }

    /**
     * Audits {@code deviceButtonMaps} against {@code entries}.
     *
     * @param entries          the installation's device entries, from its own {@code DeviceMappings.xml}
     * @param deviceButtonMaps that installation's {@code DeviceButtonMaps} folder, which need not exist
     */
    public static Result audit(List<DeviceEntry> entries, Path deviceButtonMaps) throws IOException {
        List<Attachment> attached = new ArrayList<>();
        List<Path> orphaned = new ArrayList<>();
        Set<String> matchedEntries = new LinkedHashSet<>();

        for (Path file : buttonMapFilesIn(deviceButtonMaps)) {
            String stem = stemOf(file);
            String entryName = entryNamed(entries, stem);
            if (entryName == null) {
                orphaned.add(file);
            } else {
                attached.add(new Attachment(entryName, file));
                matchedEntries.add(entryName);
            }
        }

        List<String> entriesWithoutMap = entries.stream()
                .map(DeviceEntry::name)
                .filter(name -> !matchedEntries.contains(name))
                .toList();

        return new Result(List.copyOf(attached), List.copyOf(orphaned), entriesWithoutMap);
    }

    /**
     * A missing folder means no files, not an error. Horizons ships no {@code DeviceButtonMaps} folder at
     * all, and an installation nobody has labelled anything in will not have one either.
     */
    private static List<Path> buttonMapFilesIn(Path deviceButtonMaps) throws IOException {
        if (!Files.isDirectory(deviceButtonMaps)) return List.of();
        try (Stream<Path> files = Files.list(deviceButtonMaps)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(SUFFIX))
                    .sorted()
                    .toList();
        }
    }

    /**
     * The entry this filename belongs to, or {@code null} when none does.
     * <p>
     * Matched without regard to case, because the filesystem the game reads these from does not distinguish
     * it: {@code vpcpanel.buttonMap} and {@code VPCPanel.buttonMap} cannot both exist in one folder on
     * Windows. The entry's own spelling is returned, so what is reported back matches the XML rather than
     * whatever case the file happens to use.
     */
    private static String entryNamed(List<DeviceEntry> entries, String stem) {
        String wanted = stem.toLowerCase(Locale.ROOT);
        return entries.stream()
                .map(DeviceEntry::name)
                .filter(name -> name.toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .orElse(null);
    }

    private static String stemOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - SUFFIX.length());
    }
}
