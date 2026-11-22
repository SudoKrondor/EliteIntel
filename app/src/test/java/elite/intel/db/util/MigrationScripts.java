package elite.intel.db.util;

import org.jdbi.v3.core.Handle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs one migration from {@code db-migration} against a test database, for the tests of a single migration. Shared so
 * the copies cannot drift apart (S2b review, 2026-10-25).
 */
final class MigrationScripts {

    private MigrationScripts() {
    }

    /**
     * Runs a migration a piece at a time, <strong>exactly as {@code DatabaseMigrator} does</strong> - split on a
     * semicolon at a line end, comments left in. Stripping them here once hid a migration whose last piece was
     * comments only: SQLite prepared it as an empty statement, the connection could not be closed, and every test
     * touching the database failed (Storage S4b, 2026-10-28).
     */
    static void run(Handle handle, String file) throws IOException {
        String sql;
        try (InputStream input = MigrationScripts.class.getResourceAsStream("/db-migration/" + file)) {
            assertNotNull(input, file);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        for (String statement : sql.split(";\\s*\n")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) handle.execute(trimmed);
        }
    }
}
