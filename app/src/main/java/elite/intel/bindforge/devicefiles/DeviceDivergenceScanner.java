package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.install.GameInstallation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the device files of every installation and reports what disagrees, ranked.
 * <p>
 * Assembles the three readings the ranking needs - each installation's entries, each installation's orphaned
 * button maps, and the entry names the bindings use - so that callers have one thing to ask.
 */
public final class DeviceDivergenceScanner {

    private static final Logger log = LogManager.getLogger(DeviceDivergenceScanner.class);

    private DeviceDivergenceScanner() {
    }

    /**
     * @param controlSchemesByInstall each installation's {@code ControlSchemes} folder, keyed by whatever the
     *                                caller calls that installation - that key is what appears in the findings
     * @param bindingsFolder          the one shared folder holding the {@code .binds} files
     * @return the findings, worst first, or empty when everything agrees
     */
    public static List<DeviceDivergence.Finding> scan(Map<String, Path> controlSchemesByInstall,
                                                      Path bindingsFolder) {

        Map<String, List<DeviceEntry>> entriesByInstall = new LinkedHashMap<>();
        Map<String, List<Path>> orphansByInstall = new LinkedHashMap<>();

        controlSchemesByInstall.forEach((install, controlSchemes) -> {
            Optional<List<DeviceEntry>> entries = entriesOf(install, controlSchemes);
            // WHY: an installation whose file could not be read is left out of both maps entirely rather
            // than added with no entries. Added empty, it would be indistinguishable from an installation
            // that genuinely has none, and every entry the others hold would be reported missing from it.
            if (entries.isEmpty()) return;
            entriesByInstall.put(install, entries.get());
            orphansByInstall.put(install, orphansOf(install, entries.get(), controlSchemes));
        });

        return DeviceDivergence.rank(
                DeviceMappingsComparison.compare(entriesByInstall),
                orphansByInstall,
                referencedNames(bindingsFolder));
    }

    /**
     * The installation's device entries, or empty when its file could not be read.
     * <p>
     * An empty {@code Optional} means "could not be read" and is not the same as an empty list, which means
     * "read, and holds no entries". The caller drops the first from the comparison and keeps the second,
     * because an installation that genuinely has no entries really does differ from the others.
     */
    private static Optional<List<DeviceEntry>> entriesOf(String install, Path controlSchemes) {
        try {
            return Optional.of(DeviceMappingsParser.parseIfPresent(GameInstallation.deviceMappingsIn(controlSchemes)));
        } catch (IOException e) {
            log.warn("Could not read device entries for {}: {}", install, e.getMessage());
            return Optional.empty();
        }
    }

    private static List<Path> orphansOf(String install, List<DeviceEntry> entries, Path controlSchemes) {
        try {
            return ButtonMapAudit.audit(entries, GameInstallation.deviceButtonMapsIn(controlSchemes)).orphaned();
        } catch (IOException e) {
            log.warn("Could not audit button maps for {}: {}", install, e.getMessage());
            return List.of();
        }
    }

    /**
     * With nothing readable in the bindings folder every finding falls to yellow, because severity is judged
     * against the bindings and there are none to judge against. That is the honest answer rather than a guess
     * in either direction.
     */
    private static Set<String> referencedNames(Path bindingsFolder) {
        try {
            return BindsDeviceReferences.referencedEntryNames(bindingsFolder);
        } catch (IOException e) {
            log.warn("Could not read bindings in {}: {}", bindingsFolder, e.getMessage());
            return Set.of();
        }
    }
}
