package elite.intel.appdata;

import java.util.List;

/**
 * What a Backup Settings and Data zip holds, and where each part lives in the app data folder.
 * <p>
 * The zip mirrors the folder: {@value #DB_FOLDER}/ holds a snapshot of every database file, each of
 * {@link #FOLDERS} is copied as it is, {@value #INFO_FILE} says what made it, and {@value #SEALED_KEY} - only when
 * the commander chose a password - holds {@code secret.key}, locked ({@link SealedKey}).
 */
final class AppDataArchive {

    /**
     * The format this app writes and the only one it restores.
     */
    static final String FORMAT = "1";

    static final String INFO_FILE = "backup-info.txt";
    static final String SEALED_KEY = "secret.key.sealed";
    static final String DB_FOLDER = "db";
    static final String SHARED_DATABASE = "database.db";
    static final String DATABASE_SUFFIX = ".db";
    static final String COMMANDER_DATABASE_PREFIX = "cmdr_";

    /**
     * The folders copied as they are, beside the databases.
     * <p>
     * WHY a list rather than the whole app data folder: that folder also collects what is not the commander's data -
     * the diagnostics harness, hand-made database copies, and this feature's own restore folders. A new folder in
     * {@code AppPaths} that holds the commander's data must be added here.
     * <p>
     * WHY BindForge's working copy ({@code AppPaths.getWorkingCopyDir()}) is left out, neither backed up nor
     * restored: it is BindForge's master, the side it judges the game's live files against. A restore over it would
     * put back a working copy older than the live files, and BindForge would then reconcile the newer files toward the
     * older ones (Alan, 2026-10-31). BindForge keeps its own backups of it. An older zip that carries the folder still
     * restores - {@link #isBackupEntry} skips it.
     */
    static final List<String> FOLDERS = List.of("custom-commands", "playerbackups", "themes");

    /**
     * The folders of {@link #FOLDERS} a restore adds to rather than replaces: what the backup holds goes in beside
     * what this computer has, and a file here of the same name is kept.
     * <p>
     * WHY BindForge's backups ({@code AppPaths.getPlayerBackupsDir()}): they go with the working copy, which a restore
     * leaves alone. Replaced, this computer would lose the backups of its own game files - the first setup backup,
     * kept forever, and every before-changes backup taken since the zip was made - while its working copy stayed.
     * Added to, the old computer's backups are there for RESTORE too. Each is a zip named with its date stamp
     * ({@code Backups}), so the two sets do not collide.
     */
    static final List<String> MERGED_FOLDERS = List.of("playerbackups");

    /**
     * A restore unpacks here while it is checked and unlocked.
     */
    static final String STAGING_FOLDER = "restore-pending.tmp";
    /**
     * A checked restore waits here for the next start, which swaps it in before the database opens.
     */
    static final String PENDING_FOLDER = "restore-pending";
    /**
     * What a restore replaced is moved into a folder named this plus the time, never deleted outright.
     */
    static final String SAFETY_FOLDER_PREFIX = "restore-safety-";
    /**
     * The unlocked key, inside the staging folder.
     */
    static final String KEY_FILE = "secret.key";

    static final String INFO_FORMAT = "format";
    static final String INFO_APP_VERSION = "appVersion";
    static final String INFO_CREATED = "created";
    static final String INFO_OS = "os";

    /**
     * Zip entry names always use a forward slash, whatever the platform.
     */
    static final char ENTRY_SEPARATOR = '/';

    private AppDataArchive() {
    }

    /**
     * Whether a zip entry name belongs to a backup. Anything else in the zip is not unpacked.
     */
    static boolean isBackupEntry(String name) {
        if (name.equals(INFO_FILE) || name.equals(SEALED_KEY)) return true;
        if (name.startsWith(DB_FOLDER + ENTRY_SEPARATOR)) return true;
        return FOLDERS.stream().anyMatch(folder -> name.startsWith(folder + ENTRY_SEPARATOR));
    }
}
