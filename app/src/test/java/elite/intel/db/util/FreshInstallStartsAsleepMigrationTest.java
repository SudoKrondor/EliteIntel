package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static elite.intel.db.util.MigrationScripts.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Migration {@code 12021}: an install with no language model set up starts asleep, so a noisy room cannot trigger VEGA
 * during setup. A commander who has set up either kind of model keeps the Sleep/Wake state they chose.
 */
class FreshInstallStartsAsleepMigrationTest {

    @Test
    void onlyAnInstallWithNoModelAndNoKeyIsPutToSleep() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("""
                    CREATE TABLE game_session (
                        id INTEGER PRIMARY KEY,
                        lmStudioCommandModel VARCHAR(255) NOT NULL DEFAULT '',
                        encryptedLLMKey TEXT,
                        sleepWake INTEGER NOT NULL DEFAULT 0)""");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");
            handle.execute("INSERT INTO game_session (id, lmStudioCommandModel) VALUES (2, '   ')");
            handle.execute("INSERT INTO game_session (id, encryptedLLMKey) VALUES (3, '')");
            handle.execute("INSERT INTO game_session (id, lmStudioCommandModel) VALUES (4, 'gemma-4-e4b')");
            handle.execute("INSERT INTO game_session (id, encryptedLLMKey) VALUES (5, 'c2VhbGVk')");

            run(handle, "12021__fresh_install_starts_asleep.sql");

            assertEquals(1, sleepWake(handle, 1), "a new install starts asleep");
            assertEquals(1, sleepWake(handle, 2), "a blank model name is no model");
            assertEquals(1, sleepWake(handle, 3), "an empty key is no key");
            assertEquals(0, sleepWake(handle, 4), "a commander with a local model stays awake");
            assertEquals(0, sleepWake(handle, 5), "a commander with a cloud key stays awake");
        }
    }

    private static int sleepWake(Handle handle, int id) {
        return handle.createQuery("SELECT sleepWake FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Integer.class)
                .one();
    }
}
