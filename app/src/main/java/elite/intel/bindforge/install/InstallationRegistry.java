package elite.intel.bindforge.install;

import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.db.managers.BindForgeInstallationsManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Keeps the stored installation list in step with what is on disk.
 * <p>
 * Detection runs again every time, but the list is never rebuilt from it. Rows persist because things hang
 * off them - device records are keyed by installation - and because a row carries facts detection cannot
 * supply: that the user added it by hand, or moved it.
 */
public class InstallationRegistry {

    private final GameInstallationProvider provider;
    private final BindForgeInstallationsManager installations;

    public InstallationRegistry(GameInstallationProvider provider) {
        this(provider, BindForgeInstallationsManager.getInstance());
    }

    InstallationRegistry(GameInstallationProvider provider, BindForgeInstallationsManager installations) {
        this.provider = provider;
        this.installations = installations;
    }

    /**
     * Runs detection and folds the result into the stored list: anything new is added, and every row is
     * marked present or missing according to whether its folder is there.
     *
     * @return the list as it now stands, including rows detection did not produce
     */
    public List<InstallationRow> rescan() {
        for (GameInstallation detected : provider.findInstallations()) {
            installations.record(detected.storefront().name(), detected.root(), false);
        }
        refreshMissingFlags();
        return installations.findAll();
    }

    /**
     * Adds an installation the user chose themselves.
     * <p>
     * For a Frontier-launcher install this is the only way in: nothing on the machine records where one went,
     * so detection cannot find it once it is off the published defaults.
     *
     * @throws IllegalArgumentException if the folder is not an Elite Dangerous installation
     */
    public InstallationRow addByHand(Path root) {
        // WHY: validated before it is accepted, because an arbitrary folder is not an installation and a bad
        // row would send every backup, restore and apply at the wrong place.
        if (!GameInstallation.looksLikeAnInstall(root)) {
            throw new IllegalArgumentException("Not an Elite Dangerous installation: " + root
                    + " (expected " + GameInstallation.controlSchemesUnder(root) + ")");
        }
        return installations.record(Storefront.MANUAL.name(), root, true);
    }

    public List<InstallationRow> current() {
        return installations.findAll();
    }

    /**
     * Marks each row present or missing by looking for its folder.
     * <p>
     * Judged by the filesystem rather than by whether detection returned the row. A hand-added Frontier
     * install is never produced by detection, so treating absence from that list as missing would mark it
     * missing on every single rescan.
     */
    private void refreshMissingFlags() {
        for (InstallationRow row : installations.findAll()) {
            boolean missing = !Files.isDirectory(Path.of(row.rootPath()));
            if (missing != row.missing()) {
                installations.setMissing(row.id(), missing);
            }
        }
    }
}
