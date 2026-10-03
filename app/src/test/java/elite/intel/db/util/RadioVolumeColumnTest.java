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
 * The radio level is a setting of its own, beside the ship's voice: a commander who turned the traffic down
 * must find it still down next launch, and one who never touched it must hear the radio at the level every
 * build so far has used. Both depend on the column surviving the DAO's INSERT OR REPLACE, which rewrites
 * every column of the row.
 */
class RadioVolumeColumnTest {

    private static final int SHIPPED_LEVEL = 100;

    @AfterEach
    void restoreShippedLevel() {
        SystemSession.getInstance().setRadioVolume(SHIPPED_LEVEL);
    }

    @Test
    void migrationLeavesEveryInstallationAtTheShippedLevel() throws IOException {
        try (Handle handle = Jdbi.create("jdbc:sqlite::memory:").open()) {
            handle.execute("CREATE TABLE game_session (id INTEGER PRIMARY KEY)");
            handle.execute("INSERT INTO game_session (id) VALUES (1)");

            for (String statement : loadMigration().split(";")) {
                if (!statement.isBlank()) {
                    handle.execute(statement);
                }
            }

            assertEquals(SHIPPED_LEVEL, radioVolume(handle, 1),
                    "an existing commander hears the radio at the level they have today");
            handle.execute("INSERT INTO game_session (id) VALUES (2)");
            assertEquals(SHIPPED_LEVEL, radioVolume(handle, 2), "a fresh install starts at full level");
        }
    }

    @Test
    void aChosenLevelSurvivesTheRoundTripAndIsIndependentOfTheVoice() {
        SystemSession session = SystemSession.getInstance();
        int voice = session.getVoiceVolume();

        session.setRadioVolume(40);
        assertEquals(40, session.getRadioVolume());
        assertEquals(voice, session.getVoiceVolume(), "the ship's own voice is not the radio's level");

        // Any setter goes through the same load-mutate-save cycle; if the column were missing from the
        // DAO's statement this would quietly snap the level back to full.
        boolean noiseReduction = session.isNoiseReductionEnabled();
        session.setNoiseReductionEnabled(!noiseReduction);
        try {
            assertEquals(40, session.getRadioVolume(), "saving an unrelated setting must not reset the level");
        } finally {
            session.setNoiseReductionEnabled(noiseReduction);
        }
    }

    private static int radioVolume(Handle handle, int id) {
        return handle.createQuery("SELECT radioVolume FROM game_session WHERE id = :id")
                .bind("id", id)
                .mapTo(Integer.class)
                .one();
    }

    private static String loadMigration() throws IOException {
        try (InputStream input = RadioVolumeColumnTest.class.getResourceAsStream(
                "/db-migration/01057__radio_volume.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
