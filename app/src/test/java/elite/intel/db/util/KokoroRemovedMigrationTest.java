package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KokoroRemovedMigrationTest {

    @Test
    void aKokoroCommanderMovesToSupertonicAndGoogleStays() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY, ttsProvider TEXT NOT NULL, kokoroVoice TEXT)");
            handle.execute("INSERT INTO game_session (id, ttsProvider) VALUES (1, 'KOKORO'), (2, 'GOOGLE'), (3, 'SUPERTONIC')");

            run(handle, "12015__kokoro_removed.sql");

            assertEquals("SUPERTONIC", provider(handle, 1), "Kokoro is gone, Supertonic speaks in its place");
            assertEquals("GOOGLE", provider(handle, 2));
            assertEquals("SUPERTONIC", provider(handle, 3));
            assertFalse(handle.createQuery("SELECT name FROM pragma_table_info('game_session')")
                    .mapTo(String.class).list().contains("kokoroVoice"));
        }
    }

    @Test
    void theThreeKokoroOnlyShipVoicesBecomeSupertonicsDefaultAndTheRestStay() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE ship (id INTEGER PRIMARY KEY, voice TEXT NOT NULL)");
            handle.execute("INSERT INTO ship (id, voice) VALUES (1, 'ES_DORA'), (2, 'IT_SARA'), (3, 'PT_DORA'), "
                    + "(4, 'GEORGE'), (5, 'ES_ALEX')");

            run(handle, "commander/12001__kokoro_only_ship_voices.sql");

            assertEquals(List.of("FR_SIWIS", "FR_SIWIS", "FR_SIWIS", "GEORGE", "ES_ALEX"),
                    handle.createQuery("SELECT voice FROM ship ORDER BY id").mapTo(String.class).list());
        }
    }

    private static String provider(Handle handle, int id) {
        return handle.createQuery("SELECT ttsProvider FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(String.class)
                .one();
    }

    /**
     * Splits the file exactly as {@link DatabaseMigrator} does - on a semicolon at the end of a line.
     */
    private static void run(Handle handle, String migration) throws IOException {
        try (InputStream input = KokoroRemovedMigrationTest.class.getResourceAsStream("/db-migration/" + migration)) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            for (String statement : sql.split(";\\s*\n")) {
                if (!statement.isBlank()) handle.execute(statement.trim());
            }
        }
    }
}
