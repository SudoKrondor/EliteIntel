package elite.intel.db.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        settings.setAutoBackupOnLaunch(true);
        settings.setBackupDestination(null);
        settings.setBackupRetentionDays(30);
        settings.setEditHistoryRetention(10);
    }

    @Test
    void defaultsAreWhatTheMigrationDeclares() {
        assertTrue(settings.isAutoBackupOnLaunch(), "auto-backup on launch defaults to enabled");
        assertEquals(30, settings.getBackupRetentionDays(), "Player backups are kept for 30 days by default");
        assertEquals(10, settings.getEditHistoryRetention(), "Edit History keeps 10 versions by default");
        assertNull(settings.getBackupDestination(), "no folder chosen means no stored destination");
    }

    /**
     * Each setter is followed by reading back <em>every</em> setting, because the defect this guards against
     * is a save that quietly drops a column it did not list.
     */
    @Test
    void changingOneSettingLeavesTheOthersAlone() {
        settings.setAutoBackupOnLaunch(false);
        settings.setBackupDestination("D:\\EliteBackups");
        settings.setBackupRetentionDays(7);
        settings.setEditHistoryRetention(25);

        assertFalse(settings.isAutoBackupOnLaunch());
        assertEquals("D:\\EliteBackups", settings.getBackupDestination());
        assertEquals(7, settings.getBackupRetentionDays());
        assertEquals(25, settings.getEditHistoryRetention());

        settings.setEditHistoryRetention(3);

        assertFalse(settings.isAutoBackupOnLaunch(), "still off after an unrelated save");
        assertEquals("D:\\EliteBackups", settings.getBackupDestination(), "still set after an unrelated save");
        assertEquals(7, settings.getBackupRetentionDays(), "still set after an unrelated save");
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

    /**
     * Zero days would make every backup older than the limit the instant it was written, so pruning would
     * delete the backup it had just taken.
     */
    @Test
    void aRetentionThatWouldDeleteTheBackupItJustTookIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setBackupRetentionDays(0));
        assertThrows(IllegalArgumentException.class, () -> settings.setBackupRetentionDays(-1));

        assertEquals(30, settings.getBackupRetentionDays(), "the bad value never reached the row");

        settings.setBackupRetentionDays(BindForgeSettingsManager.MIN_BACKUP_RETENTION_DAYS);
        assertEquals(1, settings.getBackupRetentionDays(), "one day is the floor and is accepted");
    }

    /**
     * A path the platform cannot represent is refused by the setter, so it never reaches the row. Stored, it
     * would throw every time {@code PlayerBackupService} resolved the destination, launch backup included.
     */
    @Test
    void aDestinationThisPlatformCannotRepresentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings.setBackupDestination("D:\\Elite\u0000Backups"));

        assertNull(settings.getBackupDestination(), "the bad value never reached the row");
    }
}
