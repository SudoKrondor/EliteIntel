package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeSettingsDao;
import elite.intel.db.util.Database;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * BindForge's settings, read and written through {@link BindForgeSettingsDao}.
 * <p>
 * A table of BindForge's own rather than columns on {@code game_session}, so BindForge never edits the DAO and
 * session class the rest of the project changes weekly.
 */
public class BindForgeSettingsManager {

    /**
     * The range the Edit History retention setting accepts, per the File Manager specification.
     */
    public static final int MIN_EDIT_HISTORY_RETENTION = 1;
    public static final int MAX_EDIT_HISTORY_RETENTION = 30;

    /**
     * The shortest a Player Backup may be kept. See {@link #setBackupRetentionDays(int)} for why there is a
     * floor but no ceiling.
     */
    public static final int MIN_BACKUP_RETENTION_DAYS = 1;

    private static volatile BindForgeSettingsManager instance;

    private BindForgeSettingsManager() {
        // Private constructor to prevent instantiation
    }

    public static BindForgeSettingsManager getInstance() {
        if (instance == null) {
            synchronized (BindForgeSettingsManager.class) {
                if (instance == null) {
                    instance = new BindForgeSettingsManager();
                }
            }
        }
        return instance;
    }

    public boolean isAutoBackupOnLaunch() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().isAutoBackupOnLaunch());
    }

    public void setAutoBackupOnLaunch(boolean autoBackupOnLaunch) {
        update(settings -> settings.setAutoBackupOnLaunch(autoBackupOnLaunch));
    }

    /**
     * The folder the user chose for Player Backup archives, or {@code null} when they have not chosen one.
     * <p>
     * Deliberately returns the stored value rather than resolving it. {@code PlayerBackupService} reads this
     * and decides "chosen folder, else {@code AppPaths.getPlayerBackupsDir()}" in its own
     * {@code resolvePlayerBackupsDir()}, and that is the one place the decision is made - a second resolver
     * here would be a rival that can disagree with it.
     */
    public String getBackupDestination() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().getBackupDestination());
    }

    /**
     * @param backupDestination the folder to write backups to, or {@code null} to return to the default
     * @throws IllegalArgumentException if the value is not a path this platform can represent
     */
    public void setBackupDestination(String backupDestination) {
        requireUsableAsPath(backupDestination);
        update(settings -> settings.setBackupDestination(backupDestination));
    }

    /**
     * How many days a Player Backup is kept before it is pruned.
     */
    public int getBackupRetentionDays() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().getBackupRetentionDays());
    }

    /**
     * @param backupRetentionDays how many days to keep a Player Backup, at least one
     * @throws IllegalArgumentException if fewer than one day
     */
    public void setBackupRetentionDays(int backupRetentionDays) {
        // WHY: a floor of one day, not a range the specification gives. Zero or negative would mean every
        // backup is older than the limit the moment it is written, so pruning would delete the backup that
        // had just been taken - the one outcome a backup feature must never produce. No upper bound: keeping
        // backups for longer costs disk, which is the user's to spend.
        if (backupRetentionDays < MIN_BACKUP_RETENTION_DAYS) {
            throw new IllegalArgumentException("Player backups must be kept for at least "
                    + MIN_BACKUP_RETENTION_DAYS + " day, was " + backupRetentionDays);
        }
        update(settings -> settings.setBackupRetentionDays(backupRetentionDays));
    }

    public int getEditHistoryRetention() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().getEditHistoryRetention());
    }

    /**
     * @param editHistoryRetention how many versions Edit History keeps per file, 1 to 30
     * @throws IllegalArgumentException if the value is outside that range
     */
    public void setEditHistoryRetention(int editHistoryRetention) {
        if (editHistoryRetention < MIN_EDIT_HISTORY_RETENTION || editHistoryRetention > MAX_EDIT_HISTORY_RETENTION) {
            throw new IllegalArgumentException("Edit History retention must be between "
                    + MIN_EDIT_HISTORY_RETENTION + " and " + MAX_EDIT_HISTORY_RETENTION
                    + ", was " + editHistoryRetention);
        }
        update(settings -> settings.setEditHistoryRetention(editHistoryRetention));
    }

    // WHY: rejected here rather than where it is read. The value is persisted, so an unparseable path stored
    // now would throw on every later read - including the backup that runs at launch - far from the caller
    // that supplied it.
    private void requireUsableAsPath(String backupDestination) {
        if (backupDestination == null) return;
        try {
            // WHY: parsed for the exception, not the result. Path.of is the only thing that knows what this
            // platform can represent, so asking it is the check - there is no value to keep.
            Path.of(backupDestination);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Not a usable folder path: " + backupDestination, e);
        }
    }

    // WHY: read-modify-write in one DAO call. Saving writes the whole row, so changing one setting from a
    // stale copy would overwrite every other setting with whatever it held when it was read.
    private void update(Consumer<BindForgeSettingsDao.BindForgeSettings> change) {
        Database.withDao(BindForgeSettingsDao.class, dao -> {
            BindForgeSettingsDao.BindForgeSettings settings = dao.get();
            change.accept(settings);
            dao.save(settings);
            return Void.TYPE;
        });
    }
}
