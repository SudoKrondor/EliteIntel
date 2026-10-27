package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.bindforge.install.GameInstallationProvider;
import elite.intel.bindforge.install.InstallationRegistry;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import elite.intel.session.PlayerSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Where the files a backup copies are: the bindings folder Elite-Intel is set to, and every installation.
 *
 * @param bindingsFolder the bindings folder Elite-Intel is set to - every {@code .binds} and
 *                       {@code StartPreset.N.start} the user edits is here
 * @param installations  every installation in the stored list, in its order
 */
public record BackupSources(Path bindingsFolder, List<Installation> installations) {

    public BackupSources {
        Objects.requireNonNull(bindingsFolder, "bindingsFolder");
        installations = List.copyOf(installations);
    }

    /**
     * One installation.
     *
     * @param storefront     a label for a person reading the backup, never a key
     * @param root           the folder holding {@code Products}
     * @param missing        whether the stored list marks its folder as gone
     * @param bindingsFolder its own bindings folder when detection reported one - on Linux each installation has
     *                       its own, inside its Proton prefix - or {@code null}
     */
    public record Installation(String storefront, Path root, boolean missing, Path bindingsFolder) {

        public Installation {
            Objects.requireNonNull(storefront, "storefront");
            Objects.requireNonNull(root, "root");
        }

        /** Its {@code ControlSchemes} folder, whether or not it is there. */
        public Path controlSchemes() {
            return GameInstallation.controlSchemesUnder(root);
        }

        /**
         * Whether it can be backed up and written now: the stored list does not mark it missing, and its
         * {@code ControlSchemes} folder is there. <strong>The one test for "reachable"</strong> - first-time startup,
         * the backup, the write and RESTORE all ask it (Krondor's review, 2026-10-27). Touches the disk.
         */
        public boolean reachable() {
            return isReachable(missing, root);
        }
    }

    /** {@link Installation#reachable()} for a stored row - what first-time startup reads. Touches the disk. */
    static boolean reachable(InstallationRow row) {
        return isReachable(row.missing(), Path.of(row.rootPath()));
    }

    private static boolean isReachable(boolean missing, Path root) {
        return !missing && Files.isDirectory(GameInstallation.controlSchemesUnder(root));
    }

    /**
     * Runs installation detection, <strong>records what it found in the stored list</strong>, and returns the bindings
     * folder and every stored installation - so a backup taken before first-time startup has ever run still covers
     * every installation on the machine (storage.md rule 12). Touches the disk and the database - not for the EDT.
     */
    public static BackupSources rescan() {
        Path bindingsFolder = PlayerSession.getInstance().getBindingsDir();
        GameInstallationProvider provider = GameInstallationProvider.forThisPlatform(bindingsFolder);
        List<GameInstallation> detected = provider.findInstallations();
        List<InstallationRow> rows = new InstallationRegistry(provider).rescan(detected);
        return of(bindingsFolder, rows, detected);
    }

    /**
     * Exactly the installations given, each with the bindings folder detection reports for it - first-time startup's
     * write, which covers the installations it read and no other (storage.md rule 1). An installation unreachable when
     * it was read is marked missing, so it is neither backed up nor written even if it is back by now.
     *
     * @param reachable   the installations read, in order
     * @param unreachable the installations that could not be read
     * @param detected    what detection found, for each installation's own bindings folder on Linux
     */
    static BackupSources of(Path bindingsFolder, List<InstallationRow> reachable, List<InstallationRow> unreachable,
                            List<GameInstallation> detected) {
        List<InstallationRow> rows = new ArrayList<>(reachable);
        unreachable.forEach(row -> rows.add(row.missing() ? row : asMissing(row)));
        return of(bindingsFolder, rows, detected);
    }

    private static InstallationRow asMissing(InstallationRow row) {
        return new InstallationRow(row.id(), row.storefront(), row.rootPath(), row.addedByHand(), true,
                row.missedWrite());
    }

    /**
     * Joins the stored list to what detection found: a stored installation detection also found takes the bindings
     * folder detection reports for it. One added by hand has none, and is covered by the folder Elite-Intel is set
     * to.
     */
    static BackupSources of(Path bindingsFolder, List<InstallationRow> rows, List<GameInstallation> detected) {
        Map<Path, Path> bindingsByRoot = new HashMap<>();
        for (GameInstallation installation : detected) {
            bindingsByRoot.put(normal(installation.root()), installation.bindingsFolder());
        }
        List<Installation> installations = rows.stream()
                .map(row -> {
                    Path root = Path.of(row.rootPath());
                    return new Installation(row.storefront(), root, row.missing(), bindingsByRoot.get(normal(root)));
                })
                .toList();
        return new BackupSources(bindingsFolder, installations);
    }

    private static Path normal(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
