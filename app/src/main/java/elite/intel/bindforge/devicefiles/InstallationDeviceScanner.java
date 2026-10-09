package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.managers.BindForgeDeviceInstallsManager;
import elite.intel.db.managers.BindForgeDeviceInstallsManager.FoundDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Records what each installation's {@code DeviceMappings.xml} actually holds.
 * <p>
 * These rows are the only copy of <em>reality</em>. The master records what the user chose; this records what
 * is on disk, and the difference between them is drift. Without it a game patch that replaces an
 * installation's file is indistinguishable from a device that was never configured - which is the incident
 * BindForge exists for.
 * <p>
 * Reads and writes one installation at a time. An installation that cannot be read is left exactly as it was
 * rather than recorded as empty, because an empty installation is a finding and an unreadable one is not.
 */
public final class InstallationDeviceScanner {

    private static final Logger log = LogManager.getLogger(InstallationDeviceScanner.class);

    private final FrontierStockDevices stock;
    private final BindForgeDeviceInstallsManager installs;

    public InstallationDeviceScanner() {
        this(FrontierStockDevices.getInstance(), BindForgeDeviceInstallsManager.getInstance());
    }

    InstallationDeviceScanner(FrontierStockDevices stock, BindForgeDeviceInstallsManager installs) {
        this.stock = stock;
        this.installs = installs;
    }

    /**
     * Reads one installation's device files and records what is there, replacing whatever was recorded
     * before.
     *
     * @param installId      the installation these rows describe
     * @param controlSchemes that installation's {@code ControlSchemes} folder
     * @return what was found and recorded, or empty when the files could not be read
     */
    public List<FoundDevice> scan(long installId, Path controlSchemes) {
        List<FoundDevice> found;
        try {
            found = read(controlSchemes);
        } catch (IOException e) {
            // WHY: nothing is written on a failed read. Recording an unreadable installation as holding no
            // devices would store the signature of a wiped file, which is exactly the alarm these rows exist
            // to raise - BindForge would be manufacturing the incident it is meant to detect.
            log.warn("Could not read device files in {}, leaving its recorded devices as they were: {}",
                    controlSchemes, e.getMessage());
            return List.of();
        }
        installs.replaceForInstall(installId, found);
        return found;
    }

    /**
     * What the installation's files say, without touching the database.
     * <p>
     * Separate from {@link #scan} so the reading can be tested against real files without a database, and so
     * a caller that only wants to look is not obliged to write.
     */
    public List<FoundDevice> read(Path controlSchemes) throws IOException {
        List<DeviceEntry> entries = DeviceMappingsParser.parseIfPresent(
                GameInstallation.deviceMappingsIn(controlSchemes));
        Set<String> withButtonMap = buttonMapOwners(entries, controlSchemes);

        List<FoundDevice> found = new ArrayList<>();
        for (DeviceEntry entry : entries) {
            // WHY: one row per entry, keyed by its element tag - the identity the file itself uses. An entry
            // owns a set of VID/PID pairs rather than one, so its own pair is recorded and the alternatives are
            // not: GamePad alone carries eighty, and they describe one entry rather than eighty devices. An element
            // with no pair of its own records none, rather than an alternative standing in for it.
            DeviceEntry.HardwareId hardware = entry.primary();
            found.add(new FoundDevice(
                    entry.name(),
                    hardware == null ? null : hardware.vid(),
                    hardware == null ? null : hardware.pid(),
                    ProvenanceRule.of(entry.name(), stock),
                    withButtonMap.contains(entry.name())));
        }
        return found;
    }

    /**
     * The entries that have a {@code .buttonMap} of their own in this installation.
     * <p>
     * It matters because a rename has to rename that file too, in every installation that holds one. Having
     * none is the normal case rather than a fault - Frontier ships button maps for two of its 51 entries.
     */
    private static Set<String> buttonMapOwners(List<DeviceEntry> entries, Path controlSchemes) throws IOException {
        ButtonMapAudit.Result audit =
                ButtonMapAudit.audit(entries, GameInstallation.deviceButtonMapsIn(controlSchemes));
        Set<String> owners = new LinkedHashSet<>();
        audit.attached().forEach(attachment -> owners.add(attachment.deviceName()));
        return owners;
    }
}
