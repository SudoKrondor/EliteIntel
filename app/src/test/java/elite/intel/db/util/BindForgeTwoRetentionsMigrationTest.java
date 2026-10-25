package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Two retention settings replace the one (file-manager.md, <em>Settings</em>; Alan, 2026-10-25): daily backups kept by
 * age, 30 days by default, and before-changes backups kept by count, 100 by default. The old value is not carried
 * over, and the other settings survive.
 */
class BindForgeTwoRetentionsMigrationTest {

    @Test
    void theOldRetentionGoesTheTwoNewOnesTakeTheirDefaultsAndTheOtherSettingsStay() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            run(handle, "12000__bindforge_settings.sql");
            run(handle, "12010__bindforge_settings_auto_backup_removed.sql");
            handle.execute("UPDATE bindforge_settings SET backup_destination = 'D:\\Backups', backup_retention_days = 7, "
                    + "edit_history_retention = 4 WHERE id = 1");

            run(handle, "12011__bindforge_settings_two_retentions.sql");

            List<String> columns = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_settings')")
                    .mapTo(String.class).list();
            assertFalse(columns.contains("backup_retention_days"), columns.toString());
            assertEquals(30, handle.createQuery("SELECT daily_backup_retention_days FROM bindforge_settings")
                    .mapTo(Integer.class).one(), "the old 7 days is not carried over");
            assertEquals(100, handle.createQuery("SELECT before_changes_backups_kept FROM bindforge_settings")
                    .mapTo(Integer.class).one());
            assertEquals("D:\\Backups", handle.createQuery("SELECT backup_destination FROM bindforge_settings")
                    .mapTo(String.class).one());
            assertEquals(4, handle.createQuery("SELECT edit_history_retention FROM bindforge_settings")
                    .mapTo(Integer.class).one(), "Edit History's setting stays until Edit History goes");
        }
    }

    /** Runs a migration a statement at a time, as {@code DatabaseMigrator} does. */
    private static void run(Handle handle, String file) throws IOException {
        String sql;
        try (InputStream input = BindForgeTwoRetentionsMigrationTest.class.getResourceAsStream("/db-migration/" + file)) {
            assertNotNull(input, file);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        for (String statement : sql.split(";\\s*\n")) {
            String trimmed = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
            if (!trimmed.isEmpty()) handle.execute(trimmed);
        }
    }
}
