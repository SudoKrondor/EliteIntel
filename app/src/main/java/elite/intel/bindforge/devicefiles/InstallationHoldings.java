package elite.intel.bindforge.devicefiles;

import elite.intel.bindforge.devicefiles.DeviceFilesPush.Target;
import elite.intel.bindforge.install.GameInstallation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Whether any installation may still hold a name - what settles a removal or a rename after catch-up (Alan,
 * 2026-10-15, A14).
 * <p>
 * Apply settles them from its own report: every installation took the push, so none holds the name. Catch-up writes
 * one installation, and its report cannot say whether the others took the removal or the rename earlier - one may
 * have been held back then and released since - so the files are read instead.
 * <p>
 * <strong>An installation that cannot be read counts as holding the name</strong>, as Apply's report counts it as not
 * having taken the push - <strong>unless it has missed no write</strong>: it took every push that carried everything,
 * up to the last one before it went out of reach. The one case that rule misjudges is a removal held back for
 * {@code .binds} at that push and released since, which then lingers there as a leftover entry the check expects.
 */
public final class InstallationHoldings implements DeviceApply.Holdings {

    private static final Logger log = LogManager.getLogger(InstallationHoldings.class);

    private final Supplier<List<Target>> targets;
    private final FrontierStockDevices stock;
    private final FrontierStockButtonMaps stockMaps;

    InstallationHoldings(Supplier<List<Target>> targets, FrontierStockDevices stock, FrontierStockButtonMaps stockMaps) {
        this.targets = targets;
        this.stock = stock;
        this.stockMaps = stockMaps;
    }

    /** The stored installations, read as they are now. */
    public static InstallationHoldings stored() {
        return new InstallationHoldings(DeviceFilesPush::storedInstallations, FrontierStockDevices.getInstance(),
                FrontierStockButtonMaps.getInstance());
    }

    /**
     * Whether some installation may still hold a pending removal - as the push removes it: one of the user's names by
     * its element or its {@code .buttonMap}; one of Frontier's by a {@code .buttonMap} that is not Frontier's own.
     */
    @Override
    public boolean mayHoldRemoval(String name) {
        return any(target -> holdsRemoval(target, name), "removal of " + name);
    }

    /**
     * Whether some installation may still hold a rename's old name - as the push finds it: an element named so,
     * ignoring case, other than one spelled exactly as the new name; or a {@code .buttonMap} under the old name that
     * is not the new name's own file.
     */
    @Override
    public boolean mayHoldOldName(String oldName, String newName) {
        return any(target -> holdsOldName(target, oldName, newName), "rename of " + oldName + " to " + newName);
    }

    @FunctionalInterface
    private interface Holds {
        boolean test(Target target) throws IOException;
    }

    private boolean any(Holds holds, String what) {
        for (Target target : targets.get()) {
            if (!target.isReachable()) {
                if (target.missedWrite()) return true;
                continue;
            }
            try {
                if (holds.test(target)) return true;
            } catch (IOException e) {
                log.info("Could not read {} to settle the {}, so it stays pending: {}", target.storefront(), what,
                        e.getMessage());
                return true;
            }
        }
        return false;
    }

    private boolean holdsRemoval(Target target, String name) throws IOException {
        Optional<byte[]> shipped = stockMaps.shippedContent(name);
        byte[] map = buttonMapContent(target, name);
        if (stock.ships(name)) {
            // WHY: Frontier's element stays; only a map BindForge or the user made goes, and Frontier's own comes back.
            return map != null && (shipped.isEmpty() || !Arrays.equals(map, shipped.get()));
        }
        return map != null || elementNames(target).contains(name);
    }

    private boolean holdsOldName(Target target, String oldName, String newName) throws IOException {
        for (String element : elementNames(target)) {
            if (element.equalsIgnoreCase(oldName) && !element.equals(newName)) return true;
        }
        Path oldMap = buttonMapPath(target, oldName);
        if (oldMap == null || !Files.isRegularFile(oldMap)) return false;
        Path newMap = buttonMapPath(target, newName);
        // WHY: on a file system that ignores case, a case-only rename finds one file under both names - the push
        // leaves it, and so must this.
        return newMap == null || !Files.exists(newMap) || !Files.isSameFile(oldMap, newMap);
    }

    private static Set<String> elementNames(Target target) throws IOException {
        byte[] mappings = DeviceFilesPush.readIfPresent(GameInstallation.deviceMappingsIn(target.controlSchemes()));
        return mappings == null ? Set.of() : DeviceMappingsParser.primaries(mappings).keySet();
    }

    private static byte[] buttonMapContent(Target target, String name) throws IOException {
        Path map = buttonMapPath(target, name);
        return map == null ? null : DeviceFilesPush.readIfPresent(map);
    }

    /** The {@code .buttonMap} under a name, or {@code null} when the name cannot be a filename there. */
    private static Path buttonMapPath(Target target, String name) {
        try {
            return GameInstallation.buttonMapIn(target.controlSchemes(), name);
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
