package elite.intel.bindforge.devices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            List<DeviceEntry> entries = entriesOf(install, controlSchemes);
            entriesByInstall.put(install, entries);
            orphansByInstall.put(install, orphansOf(install, entries, controlSchemes));
        });

        return DeviceDivergence.rank(
                DeviceMappingsComparison.compare(entriesByInstall),
                orphansByInstall,
                referencedNames(bindingsFolder));
    }

    /**
     * An installation whose files cannot be read contributes no entries.
     * <p>
     * Deliberately not treated as "this installation has nothing", which would report every other
     * installation's entries as missing from it and fill the list with red that is really one unreadable
     * file. It is logged and the installation sits the comparison out.
     */
    private static List<DeviceEntry> entriesOf(String install, Path controlSchemes) {
        try {
            return DeviceMappingsParser.parseIfPresent(controlSchemes.resolve("DeviceMappings.xml"));
        } catch (IOException e) {
            log.warn("Could not read device entries for {}: {}", install, e.getMessage());
            return List.of();
        }
    }

    private static List<Path> orphansOf(String install, List<DeviceEntry> entries, Path controlSchemes) {
        try {
            return ButtonMapAudit.audit(entries, controlSchemes.resolve("DeviceButtonMaps")).orphaned();
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
