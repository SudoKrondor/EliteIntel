package elite.intel.db.util;

import elite.intel.session.SystemSession;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The key input pacing is a per-machine setting: a commander who slowed it down because the game was
 * dropping keystrokes must find it still slowed down next launch, and one who never touched it must keep
 * the pacing every build so far has used. Both depend on the column surviving the DAO's INSERT OR REPLACE,
 * which rewrites every column of the row.
 */
class KeyInputDelayColumnTest {

    @AfterEach
    void restoreDefaultPacing() {
        SystemSession.getInstance().setKeyInputDelayMs(SystemSession.KEY_INPUT_DELAY_MIN_MS);
    }

    @Test
    void migrationLeavesEveryInstallationOnTheShippedPacing() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");

            for (String statement : loadMigration().split(";")) {
                if (!statement.isBlank()) {
                    handle.execute(statement);
                }
            }

            assertEquals(SystemSession.KEY_INPUT_DELAY_MIN_MS, keyInputDelayMs(handle, 1),
                    "an existing commander keeps the pacing they have today");
            handle.execute("INSERT INTO game_session (id) VALUES (2)");
            assertEquals(SystemSession.KEY_INPUT_DELAY_MIN_MS, keyInputDelayMs(handle, 2),
                    "a fresh install starts at FAST");
        }
    }

    @Test
    void aChosenPacingSurvivesTheRoundTrip() {
        SystemSession session = SystemSession.getInstance();
        assertEquals(SystemSession.KEY_INPUT_DELAY_MIN_MS, session.getKeyInputDelayMs(),
                "the test database starts at the shipped pacing");

        session.setKeyInputDelayMs(SystemSession.KEY_INPUT_DELAY_MAX_MS);
        assertEquals(SystemSession.KEY_INPUT_DELAY_MAX_MS, session.getKeyInputDelayMs());

        // Any setter goes through the same load-mutate-save cycle; if the column were missing from the
        // DAO's statement this would quietly snap the pacing back to FAST.
        boolean noiseReduction = session.isNoiseReductionEnabled();
        session.setNoiseReductionEnabled(!noiseReduction);
        try {
            assertEquals(SystemSession.KEY_INPUT_DELAY_MAX_MS, session.getKeyInputDelayMs(),
                    "saving an unrelated setting must not reset the pacing");
        } finally {
            session.setNoiseReductionEnabled(noiseReduction);
        }
    }

    @Test
    void aPacingOutsideTheSliderRangeIsRejected() {
        SystemSession session = SystemSession.getInstance();

        assertThrows(IllegalArgumentException.class,
                () -> session.setKeyInputDelayMs(SystemSession.KEY_INPUT_DELAY_MIN_MS - 1));
        assertThrows(IllegalArgumentException.class,
                () -> session.setKeyInputDelayMs(SystemSession.KEY_INPUT_DELAY_MAX_MS + 1));
    }

    private static int keyInputDelayMs(Handle handle, int id) {
        return handle.createQuery("SELECT keyInputDelayMs FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Integer.class)
                .one();
    }

    private static String loadMigration() throws IOException {
        try (InputStream input = KeyInputDelayColumnTest.class.getResourceAsStream(
                "/db-migration/01038__key_input_delay.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
