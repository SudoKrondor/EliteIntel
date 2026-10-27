package elite.intel.db.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * BindForge's settings survive a save.
 * <p>
 * The round trip is the point: {@code save} writes the whole row, so a column missing from the INSERT is
 * silently reset to its default the next time any other setting is changed. That has already happened once in
 * {@code global_settings}, where two columns are absent from its own save statement and nothing caught it.
 */
class BindForgeSettingsManagerTest {

    private final BindForgeSettingsManager settings = BindForgeSettingsManager.getInstance();

    // WHY: the whole suite shares one in-memory database and one bindforge_settings row, and JUnit applies no
    // method order by default. Without this, a test asserting the migration's defaults passes or fails
    // depending on which sibling ran before it and what it left behind.
    @BeforeEach
    void resetToTheMigrationDefaults() {
        settings.setBackupDestination(null);
        settings.setDailyBackupRetentionDays(30);
        settings.setBeforeChangesBackupsKept(100);
        settings.setEditHistoryRetention(10);
    }

    @Test
    void defaultsAreWhatTheMigrationDeclares() {
        assertEquals(30, settings.getDailyBackupRetentionDays(), "daily backups are kept for 30 days by default");
        assertEquals(100, settings.getBeforeChangesBackupsKept(), "the newest 100 before-changes backups by default");
        assertEquals(10, settings.getEditHistoryRetention(), "Edit History keeps 10 versions by default");
        assertNull(settings.getBackupDestination(), "no folder chosen means no stored destination");
    }

    /**
     * Each setter is followed by reading back <em>every</em> setting, because the defect this guards against
     * is a save that quietly drops a column it did not list.
     */
    @Test
    void changingOneSettingLeavesTheOthersAlone() {
        settings.setBackupDestination("D:\\EliteBackups");
        settings.setDailyBackupRetentionDays(90);
        settings.setBeforeChangesBackupsKept(250);
        settings.setEditHistoryRetention(25);

        assertEquals("D:\\EliteBackups", settings.getBackupDestination());
        assertEquals(90, settings.getDailyBackupRetentionDays());
        assertEquals(250, settings.getBeforeChangesBackupsKept());
        assertEquals(25, settings.getEditHistoryRetention());

        settings.setEditHistoryRetention(3);

        assertEquals("D:\\EliteBackups", settings.getBackupDestination(), "still set after an unrelated save");
        assertEquals(90, settings.getDailyBackupRetentionDays(), "still set after an unrelated save");
        assertEquals(250, settings.getBeforeChangesBackupsKept(), "still set after an unrelated save");
        assertEquals(3, settings.getEditHistoryRetention());
    }

    @Test
    void clearingTheDestinationReturnsToNoChoice() {
        settings.setBackupDestination("D:\\EliteBackups");
        settings.setBackupDestination(null);

        assertNull(settings.getBackupDestination());
    }

    @Test
    void retentionOutsideItsRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setEditHistoryRetention(0));
        assertThrows(IllegalArgumentException.class, () -> settings.setEditHistoryRetention(31));

        settings.setEditHistoryRetention(BindForgeSettingsManager.MIN_EDIT_HISTORY_RETENTION);
        assertEquals(1, settings.getEditHistoryRetention(), "the bottom of the range is accepted");

        settings.setEditHistoryRetention(BindForgeSettingsManager.MAX_EDIT_HISTORY_RETENTION);
        assertEquals(30, settings.getEditHistoryRetention(), "the top of the range is accepted");
    }

    /** Daily backups are kept for 30, 60, 90 or 180 days - nothing else (file-manager.md, Settings). */
    @Test
    void aDailyRetentionThatIsNotOneOfTheChoicesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setDailyBackupRetentionDays(0));
        assertThrows(IllegalArgumentException.class, () -> settings.setDailyBackupRetentionDays(45));

        assertEquals(30, settings.getDailyBackupRetentionDays(), "the bad value never reached the row");

        for (int days : BindForgeSettingsManager.DAILY_BACKUP_RETENTION_CHOICES) {
            settings.setDailyBackupRetentionDays(days);
            assertEquals(days, settings.getDailyBackupRetentionDays());
        }
    }

    /** Ten to a thousand: never so few that one evening of applies empties it. */
    @Test
    void aBeforeChangesCountOutsideItsRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setBeforeChangesBackupsKept(9));
        assertThrows(IllegalArgumentException.class, () -> settings.setBeforeChangesBackupsKept(1001));

        assertEquals(100, settings.getBeforeChangesBackupsKept(), "the bad value never reached the row");

        settings.setBeforeChangesBackupsKept(BindForgeSettingsManager.MIN_BEFORE_CHANGES_BACKUPS_KEPT);
        assertEquals(10, settings.getBeforeChangesBackupsKept(), "the bottom of the range is accepted");
        settings.setBeforeChangesBackupsKept(BindForgeSettingsManager.MAX_BEFORE_CHANGES_BACKUPS_KEPT);
        assertEquals(1000, settings.getBeforeChangesBackupsKept(), "the top of the range is accepted");
    }

    /**
     * A path the platform cannot represent is refused by the setter, so it never reaches the row. Stored, it
     * would throw every time {@code BackupDestination} resolved the destination, launch backup included.
     */
    @Test
    void aDestinationThisPlatformCannotRepresentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setBackupDestination("D:\\Elite\u0000Backups"));

        assertNull(settings.getBackupDestination(), "the bad value never reached the row");
    }
}
