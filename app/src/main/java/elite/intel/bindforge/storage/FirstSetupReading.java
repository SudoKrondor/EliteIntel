package elite.intel.bindforge.storage;

import elite.intel.bindforge.devicefiles.BindsDeviceReferences;
import elite.intel.bindforge.devicefiles.FirstSetupPlan;
import elite.intel.bindforge.devicefiles.FirstSetupReader;
import elite.intel.bindforge.devicefiles.FrontierStockButtonMaps;
import elite.intel.bindforge.devicefiles.FrontierStockDevices;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * What first-time startup found, read once (storage.md rules 1-3): every binds file and the preset file from the
 * bindings folder, every reachable installation's device files, and how they compare. Nothing is written while it is
 * looked at; the files it holds are the files the working copy is made from.
 *
 * @param bindingsFolder the bindings folder Elite-Intel is set to
 * @param bindings       every {@code .binds} and {@code StartPreset.N.start} in it, by file name
 * @param installations  every installation whose folder is there, in the stored list's order
 * @param unreachable    installations whose folder is gone - left out, and checked like any other when they come back
 * @param plan           how the installations' device files compare
 * @param references     for each binds file that could be read, how many bindings name each device - keyed as written
 */
public record FirstSetupReading(Path bindingsFolder, Map<String, byte[]> bindings,
                                List<InstallationFiles> installations, List<InstallationRow> unreachable,
                                FirstSetupPlan plan, Map<String, Map<String, Integer>> references) {

    private static final Logger log = LogManager.getLogger(FirstSetupReading.class);

    public FirstSetupReading {
        Objects.requireNonNull(bindingsFolder, "bindingsFolder");
        bindings = Collections.unmodifiableMap(new TreeMap<>(bindings));
        installations = List.copyOf(installations);
        unreachable = List.copyOf(unreachable);
        Objects.requireNonNull(plan, "plan");
        references = Collections.unmodifiableMap(new TreeMap<>(references));
    }

    /**
     * Puts a reading together from the files read. Pure apart from the log.
     *
     * @throws IOException if an installation's device files cannot be read for the plan - the message names the
     *                     installation and the file
     */
    public static FirstSetupReading of(Path bindingsFolder, Map<String, byte[]> bindings,
                                       List<InstallationFiles> installations, List<InstallationRow> unreachable,
                                       FrontierStockDevices stock, FrontierStockButtonMaps stockMaps)
            throws IOException {
        Map<String, Map<String, Integer>> references = new LinkedHashMap<>();
        Set<String> referenced = new LinkedHashSet<>();
        bindings.forEach((name, content) -> {
            if (!FileNames.isBindsFile(name)) return;
            try {
                Map<String, Integer> counts = BindsDeviceReferences.referenceCountsIn(content);
                references.put(name, counts);
                referenced.addAll(counts.keySet());
            } catch (IOException e) {
                // WHY: skipped, not fatal, as the bindings folder's own reading does - the folder demonstrably holds
                // files with a .binds name that are not binds files. It still goes into the working copy as it is.
                log.warn("Skipped {} while reading which devices the bindings name: {}", name, e.getMessage());
            }
        });

        List<FirstSetupPlan.Installation> read = new ArrayList<>();
        for (InstallationFiles files : installations) {
            try {
                read.add(FirstSetupReader.read(files.installId(), files.storefront(), files.deviceMappings(),
                        files.buttonMaps(), stock, stockMaps));
            } catch (IOException e) {
                throw new IOException(files.storefront() + " (" + files.root() + "): " + e.getMessage(), e);
            }
        }
        return new FirstSetupReading(bindingsFolder, bindings, installations, unreachable,
                new FirstSetupPlan(read, referenced), references);
    }

    /**
     * Whether every installation holds the same device files, byte for byte - nothing to reconcile and nothing to ask
     * ({@code "Agree" means identical}, alias-designer.md, <em>Activation</em>). True for one installation, or none.
     */
    public boolean identical() {
        for (InstallationFiles files : installations) {
            if (!files.sameFilesAs(installations.getFirst())) return false;
        }
        return true;
    }

    /** The installation with this id, among those read. */
    public InstallationFiles installation(long installId) {
        return installations.stream().filter(files -> files.installId() == installId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no installation " + installId + " was read"));
    }
}
