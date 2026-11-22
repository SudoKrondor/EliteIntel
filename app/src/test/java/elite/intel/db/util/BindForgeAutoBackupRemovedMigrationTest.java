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
 * The auto-backup switch is gone - the startup backup cannot be switched off (storage.md rule 12) - and the settings
 * the user did choose survive its removal.
 */
class BindForgeAutoBackupRemovedMigrationTest {

    @Test
    void theColumnGoesAndTheOtherSettingsStay() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            run(handle, "12000__bindforge_settings.sql");
            handle.execute("UPDATE bindforge_settings SET backup_destination = 'D:\\Backups', backup_retention_days = 7, "
                    + "edit_history_retention = 4, auto_backup_on_launch = 0 WHERE id = 1");

            run(handle, "12010__bindforge_settings_auto_backup_removed.sql");

            List<String> columns = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_settings')")
                    .mapTo(String.class).list();
            assertFalse(columns.contains("auto_backup_on_launch"), columns.toString());
            assertEquals("D:\\Backups", handle.createQuery("SELECT backup_destination FROM bindforge_settings")
                    .mapTo(String.class).one());
            assertEquals(7, handle.createQuery("SELECT backup_retention_days FROM bindforge_settings")
                    .mapTo(Integer.class).one());
            assertEquals(4, handle.createQuery("SELECT edit_history_retention FROM bindforge_settings")
                    .mapTo(Integer.class).one(), "Edit History's setting stays until Edit History goes");
        }
    }
}
