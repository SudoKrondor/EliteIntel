package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static elite.intel.db.util.MigrationScripts.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Migration {@code 12014} (Storage S6, Alan, 2026-10-28): the Edit History retention setting is dropped with the preset
 * file's own history, the last thing that read it, and the settings the user did choose stay.
 */
class BindForgeEditHistoryRetentionRemovedMigrationTest {

    @Test
    void theColumnGoesAndTheOtherSettingsStay() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            run(handle, "12000__bindforge_settings.sql");
            run(handle, "12010__bindforge_settings_auto_backup_removed.sql");
            run(handle, "12011__bindforge_settings_two_retentions.sql");
            handle.execute("UPDATE bindforge_settings SET backup_destination = 'D:\\Backups', "
                    + "daily_backup_retention_days = 90, before_changes_backups_kept = 250, edit_history_retention = 4 "
                    + "WHERE id = 1");

            run(handle, "12014__bindforge_settings_edit_history_retention_removed.sql");

            List<String> columns = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_settings')")
                    .mapTo(String.class).list();
            assertFalse(columns.contains("edit_history_retention"), columns.toString());
            assertEquals("D:\\Backups", handle.createQuery("SELECT backup_destination FROM bindforge_settings")
                    .mapTo(String.class).one());
            assertEquals(90, handle.createQuery("SELECT daily_backup_retention_days FROM bindforge_settings")
                    .mapTo(Integer.class).one());
            assertEquals(250, handle.createQuery("SELECT before_changes_backups_kept FROM bindforge_settings")
                    .mapTo(Integer.class).one());
        }
    }
}
