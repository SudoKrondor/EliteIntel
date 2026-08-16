package elite.intel.db.util;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoogleWaveNetPitchMigrationTest {

    @Test
    void migrationDefaultsExistingAndNewSessionsToNativePitch() throws IOException {
        try (var handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");

            handle.execute(loadMigration());

            assertEquals(0, pitch(handle, 1), "an existing commander keeps native pitch");
            handle.execute("INSERT INTO game_session (id) VALUES (2)");
            assertEquals(0, pitch(handle, 2), "a new session starts at native pitch");
        }
    }

    private static int pitch(org.jdbi.v3.core.Handle handle, int id) {
        return handle.createQuery("SELECT googleWaveNetPitch FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Integer.class)
                .one();
    }

    private static String loadMigration() throws IOException {
        try (InputStream input = GoogleWaveNetPitchMigrationTest.class.getResourceAsStream(
                "/db-migration/01028__google_wavenet_pitch.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
