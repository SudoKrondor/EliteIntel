package elite.intel.bindforge.storage;

import elite.intel.db.managers.BindForgeInstallationsManager;
import elite.intel.db.managers.BindForgeSettingsManager;
import elite.intel.session.PlayerSession;
import elite.intel.util.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Which folder backups are written to: the one the user chose, or Elite-Intel's default (file-manager.md,
 * <em>Settings</em>).
 * <p>
 * <strong>The one place that decision is made.</strong> {@code BindForgeSettingsManager} stores the raw value and
 * does not resolve it, so there is no second resolver to disagree with this one.
 * <p>
 * <strong>Anything wrong with the chosen folder falls back to the default rather than failing</strong> - a setting
 * that cannot be read, a path that cannot be made, a folder that cannot be written, or a folder inside a game folder,
 * where a game update would delete the backups (Alan, 2026-10-24). The backup is the thing being protected.
 */
public final class BackupDestination {

    private static final Logger log = LogManager.getLogger(BackupDestination.class);

    /** Reads the chosen folder; may throw. */
    @FunctionalInterface
    interface Chosen {
        String get();
    }

    /** Elite-Intel's own backup folder. */
    @FunctionalInterface
    interface Default {
        Path get() throws IOException;
    }

    private final Chosen chosen;
    private final Default fallback;
    private final Supplier<List<Path>> gameFolders;

    /** The stored setting, {@link AppPaths#getPlayerBackupsDir()}, and the stored installations and bindings folder. */
    public static BackupDestination stored() {
        return new BackupDestination(BindForgeSettingsManager.getInstance()::getBackupDestination,
                AppPaths::getPlayerBackupsDir, BackupDestination::storedGameFolders);
    }

    /**
     * @param gameFolders folders a backup must never be written inside; may throw, and is then taken as none
     */
    BackupDestination(Chosen chosen, Default fallback, Supplier<List<Path>> gameFolders) {
        this.chosen = chosen;
        this.fallback = fallback;
        this.gameFolders = gameFolders;
    }

    /**
     * The folder to write to, created if needed.
     *
     * @throws IOException only if Elite-Intel's own folder cannot be created either
     */
    public Path resolve() throws IOException {
        Path folder = chosenFolder();
        if (folder != null && usable(folder)) {
            return folder;
        }
        return fallback.get();
    }

    private Path chosenFolder() {
        try {
            String value = chosen.get();
            return value == null || value.isBlank() ? null : Path.of(value);
        } catch (RuntimeException e) {
            // WHY: broad on purpose - a database failure (JDBI's are unchecked) and a stored path this platform cannot
            // parse (InvalidPathException) both fall back to the default folder, so the backup is still taken. The
            // exception is logged whole: the fallback hides the cause from the user, so the log must not hide it too.
            log.warn("Could not read the chosen backup folder, using the default", e);
            return null;
        }
    }

    private boolean usable(Path folder) {
        Path normal = folder.toAbsolutePath().normalize();
        for (Path gameFolder : gameFoldersOrNone()) {
            if (normal.startsWith(gameFolder.toAbsolutePath().normalize())) {
                log.warn("The chosen backup folder {} is inside a game folder ({}), where a game update would delete "
                        + "it - using the default", folder, gameFolder);
                return false;
            }
        }
        try {
            Files.createDirectories(folder);
        } catch (IOException | RuntimeException e) {
            log.warn("The chosen backup folder {} cannot be created - using the default", folder, e);
            return false;
        }
        if (!Files.isWritable(folder)) {
            log.warn("The chosen backup folder {} cannot be written to - using the default", folder);
            return false;
        }
        return true;
    }

    private List<Path> gameFoldersOrNone() {
        try {
            return gameFolders.get();
        } catch (RuntimeException e) {
            // WHY: the check guards against a bad choice; failing to run it must not stop the backup.
            log.warn("Could not read the game folders to check the backup folder against", e);
            return List.of();
        }
    }

    /** Every stored installation's folder and the bindings folder Elite-Intel is set to. Reads the database. */
    private static List<Path> storedGameFolders() {
        List<Path> folders = new ArrayList<>();
        folders.add(PlayerSession.getInstance().getBindingsDir());
        for (var row : BindForgeInstallationsManager.getInstance().findAll()) {
            try {
                folders.add(Path.of(row.rootPath()));
            } catch (InvalidPathException e) {
                log.debug("Stored installation path {} is not a path here; not checked against", row.rootPath());
            }
        }
        return folders;
    }
}
