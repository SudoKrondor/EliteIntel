package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The transmission-audio migrations, run against a bare {@code game_session} so each is seen on its own.
 */
class TransmissionAudioMigrationTest {

    @Test
    void migrationDefaultsEverythingOff() throws Exception {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");
            run(handle, "11003__transmission_audio_options.sql");
            assertEquals(0, handle.createQuery("SELECT transmissionTones + enhancedRadioEffect + effectsOnRadio"
                    + " + effectsOnVegaAway + supertonicBoost FROM game_session").mapTo(Integer.class).one());
        }
    }

    @Test
    void tuningMigrationRetainsPreviousSupertonicChoice() throws Exception {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY, supertonicBoost INTEGER NOT NULL)");
            handle.execute("INSERT INTO game_session (id, supertonicBoost) VALUES (1, 1)");
            handle.execute("INSERT INTO game_session (id, supertonicBoost) VALUES (2, 0)");
            run(handle, "11004__audio_tuning_levels.sql");
            assertEquals(20, handle.createQuery("SELECT supertonicBoostPercent FROM game_session WHERE id = 1").mapTo(Integer.class).one());
            assertEquals(0, handle.createQuery("SELECT supertonicBoostPercent FROM game_session WHERE id = 2").mapTo(Integer.class).one());
            assertEquals(35, handle.createQuery("SELECT transmissionToneVolume FROM game_session WHERE id = 1").mapTo(Integer.class).one());
        }
    }

    /**
     * Splits the file exactly as {@link DatabaseMigrator} does - on a semicolon at the end of a line - so the
     * test fails the same way the app would.
     */
    private void run(Handle handle, String migration) throws Exception {
        try (InputStream resource = getClass().getResourceAsStream("/db-migration/" + migration)) {
            assertNotNull(resource, migration);
            String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            for (String statement : sql.split(";\\s*\n")) {
                if (!statement.isBlank()) handle.execute(statement.trim());
            }
        }
    }
}
