package elite.intel.db.util;

import elite.intel.session.SystemSession;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The Sleep/Wake flag is persisted, so a commander who put her to sleep finds her asleep next launch and is
 * told so out loud. That only works if the column survives the round trip through the DAO's INSERT OR REPLACE,
 * which rewrites every column of the row - a field left out of that statement reads back as its default and
 * the gate silently reopens itself on the next unrelated setting change.
 */
class SleepWakeColumnTest {

    @AfterEach
    void wakeHerBackUp() {
        SystemSession.getInstance().setSleeping(false);
    }

    @Test
    void migrationDefaultsExistingAndNewSessionsToAwake() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");

            handle.execute(loadMigration());

            assertEquals(0, sleepWake(handle, 1), "an existing commander is not put to sleep by the upgrade");
            handle.execute("INSERT INTO game_session (id) VALUES (2)");
            assertEquals(0, sleepWake(handle, 2), "a fresh install starts awake");
        }
    }

    @Test
    void sleepStateSurvivesTheRoundTrip() {
        SystemSession session = SystemSession.getInstance();
        assertFalse(session.isSleeping(), "the test database starts awake");

        session.setSleeping(true);
        assertTrue(session.isSleeping());

        session.setSleeping(false);
        assertFalse(session.isSleeping());
    }

    @Test
    void anUnrelatedSaveDoesNotReopenTheGate() {
        SystemSession session = SystemSession.getInstance();
        session.setSleeping(true);

        // Any setter goes through the same load-mutate-save cycle; if sleepWake were missing from the DAO's
        // statement this would quietly wake her up.
        boolean noiseReduction = session.isNoiseReductionEnabled();
        session.setNoiseReductionEnabled(!noiseReduction);
        try {
            assertTrue(session.isSleeping(), "saving an unrelated setting must not clear the sleep flag");
        } finally {
            session.setNoiseReductionEnabled(noiseReduction);
        }
    }

    private static int sleepWake(Handle handle, int id) {
        return handle.createQuery("SELECT sleepWake FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Integer.class)
                .one();
    }

    private static String loadMigration() throws IOException {
        try (InputStream input = SleepWakeColumnTest.class.getResourceAsStream(
                "/db-migration/01030__restore_sleep_wake.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
