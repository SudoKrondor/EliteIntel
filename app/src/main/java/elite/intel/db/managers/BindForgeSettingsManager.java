package elite.intel.db.managers;

import elite.intel.db.dao.BindForgeSettingsDao;
import elite.intel.db.util.Database;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
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

    /** The days a daily backup can be kept for (file-manager.md, <em>Settings</em>; Alan, 2026-10-25). */
    public static final List<Integer> DAILY_BACKUP_RETENTION_CHOICES = List.of(30, 60, 90, 180);

    /** The range of how many before-changes backups are kept (file-manager.md, <em>Settings</em>). */
    public static final int MIN_BEFORE_CHANGES_BACKUPS_KEPT = 10;
    public static final int MAX_BEFORE_CHANGES_BACKUPS_KEPT = 1000;

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

    /**
     * The folder the user chose for backups, or {@code null} when they have not chosen one.
     * <p>
     * Deliberately returns the stored value rather than resolving it. {@code bindforge.storage.BackupDestination}
     * reads this and decides "chosen folder, else {@code AppPaths.getPlayerBackupsDir()}", and that is the one
     * place the decision is made - a second resolver here would be a rival that can disagree with it.
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
     * How many days a daily backup is kept before retention deletes it.
     */
    public int getDailyBackupRetentionDays() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().getDailyBackupRetentionDays());
    }

    /**
     * @param days one of {@link #DAILY_BACKUP_RETENTION_CHOICES}
     * @throws IllegalArgumentException if it is not one of them
     */
    public void setDailyBackupRetentionDays(int days) {
        if (!DAILY_BACKUP_RETENTION_CHOICES.contains(days)) {
            throw new IllegalArgumentException("Daily backups are kept for one of " + DAILY_BACKUP_RETENTION_CHOICES
                    + " days, was " + days);
        }
        update(settings -> settings.setDailyBackupRetentionDays(days));
    }

    /**
     * How many before-changes backups are kept, newest first.
     */
    public int getBeforeChangesBackupsKept() {
        return Database.withDao(BindForgeSettingsDao.class, dao -> dao.get().getBeforeChangesBackupsKept());
    }

    /**
     * @param kept how many before-changes backups to keep, {@value #MIN_BEFORE_CHANGES_BACKUPS_KEPT} to
     *             {@value #MAX_BEFORE_CHANGES_BACKUPS_KEPT}
     * @throws IllegalArgumentException if outside that range
     */
    public void setBeforeChangesBackupsKept(int kept) {
        // WHY: a floor of ten - never so few that one evening of applies empties it (file-manager.md, Settings).
        if (kept < MIN_BEFORE_CHANGES_BACKUPS_KEPT || kept > MAX_BEFORE_CHANGES_BACKUPS_KEPT) {
            throw new IllegalArgumentException("Before-changes backups kept must be between "
                    + MIN_BEFORE_CHANGES_BACKUPS_KEPT + " and " + MAX_BEFORE_CHANGES_BACKUPS_KEPT + ", was " + kept);
        }
        update(settings -> settings.setBeforeChangesBackupsKept(kept));
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
