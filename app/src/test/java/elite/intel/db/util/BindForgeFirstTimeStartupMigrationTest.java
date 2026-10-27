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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration {@code 12012} (Storage S2b, 2026-10-25): an installation already in the list is not removed, the "before
 * BindForge" backup's folder is gone, and a first-time startup completed the old way - into the database master - is
 * cleared, so every tester goes through it again.
 */
class BindForgeFirstTimeStartupMigrationTest {

    @Test
    void existingInstallationsStayListedTheBackupFolderGoesAndTheFlagIsCleared() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            run(handle, "12001__bindforge_installations.sql");
            run(handle, "12002__bindforge_devices.sql");
            run(handle, "12005__bindforge_activation.sql");
            run(handle, "12006__bindforge_missed_write.sql");
            handle.execute("INSERT INTO bindforge_installations (storefront, root_path) VALUES ('STEAM', 'E:\\Steam')");
            handle.execute("UPDATE bindforge_activation SET activated_at = CURRENT_TIMESTAMP, "
                    + "backup_folder = 'C:\\backups\\x_before-bindforge' WHERE id = 1");

            run(handle, "12012__bindforge_first_time_startup.sql");

            assertEquals(0, handle.createQuery("SELECT is_removed FROM bindforge_installations")
                    .mapTo(Integer.class).one(), "an installation already listed is not removed");
            List<String> columns = handle.createQuery("SELECT name FROM pragma_table_info('bindforge_activation')")
                    .mapTo(String.class).list();
            assertFalse(columns.contains("backup_folder"), columns.toString());
            assertTrue(handle.createQuery("SELECT activated_at IS NULL FROM bindforge_activation WHERE id = 1")
                    .mapTo(Boolean.class).one(), "set up the old way, so first-time startup runs again");
        }
    }

    /** Runs a migration a statement at a time, as {@code DatabaseMigrator} does. */
    private static void run(Handle handle, String file) throws IOException {
        String sql;
        try (InputStream input = BindForgeFirstTimeStartupMigrationTest.class
                .getResourceAsStream("/db-migration/" + file)) {
            assertNotNull(input, file);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        for (String statement : sql.split(";\\s*\n")) {
            String trimmed = statement.replaceAll("(?m)^\\s*--.*$", "").trim();
            if (!trimmed.isEmpty()) handle.execute(trimmed);
        }
    }
}
