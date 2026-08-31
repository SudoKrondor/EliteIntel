package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guarantees the jukebox schema itself has to make, checked against a freshly migrated database
 * rather than the shared test one - a default is only observable before anything has been set.
 */
class JukeboxSchemaTest {

    @Test
    void aFreshInstallStartsSequentialAtAnAudibleVolumeWithNothingLoaded() {
        try (Handle handle = migratedDatabase()) {
            assertEquals(70, handle.createQuery("SELECT volume FROM jukebox_state WHERE id = 1")
                    .mapTo(Integer.class).one());
            assertEquals("SEQUENTIAL", handle.createQuery("SELECT playbackOrder FROM jukebox_state WHERE id = 1")
                            .mapTo(String.class).one(),
                    "the playlist order is the commander's own work, so nothing is shuffled unasked");
            assertEquals(0L, handle.createQuery("SELECT positionMs FROM jukebox_state WHERE id = 1")
                    .mapTo(Long.class).one());
            assertNull(handle.createQuery("SELECT musicFolder FROM jukebox_state WHERE id = 1")
                    .mapTo(String.class).one());
            assertNull(handle.createQuery("SELECT currentTrackId FROM jukebox_state WHERE id = 1")
                    .mapTo(Long.class).one());
            assertEquals(0, handle.createQuery("SELECT COUNT(*) FROM jukebox_track").mapTo(Integer.class).one());
        }
    }

    @Test
    void reapplyingTheMigrationDoesNotResetSettingsTheCommanderChose() {
        try (Handle handle = migratedDatabase()) {
            handle.execute("UPDATE jukebox_state SET volume = 20, playbackOrder = 'RANDOM' WHERE id = 1");

            applyMigration(handle);

            assertEquals(20, handle.createQuery("SELECT volume FROM jukebox_state WHERE id = 1")
                            .mapTo(Integer.class).one(),
                    "the seed is INSERT OR IGNORE, so an upgrade must not stamp on a stored choice");
        }
    }

    @Test
    void theSameFileCannotEnterThePlaylistTwice() {
        try (Handle handle = migratedDatabase()) {
            handle.execute("INSERT INTO jukebox_track (path, ordinal) VALUES ('/music/a.mp3', 0)");

            assertThrows(Exception.class, () -> handle.execute(
                            "INSERT INTO jukebox_track (path, ordinal) VALUES ('/music/a.mp3', 1)"),
                    "re-picking a folder must not double every track");
        }
    }

    @Test
    void aRemovedTracksIdIsNeverHandedToAnotherFile() {
        try (Handle handle = migratedDatabase()) {
            handle.execute("INSERT INTO jukebox_track (path, ordinal) VALUES ('/music/a.mp3', 0)");
            long firstId = handle.createQuery("SELECT id FROM jukebox_track").mapTo(Long.class).one();
            handle.execute("DELETE FROM jukebox_track");

            handle.execute("INSERT INTO jukebox_track (path, ordinal) VALUES ('/music/b.mp3', 0)");
            long secondId = handle.createQuery("SELECT id FROM jukebox_track").mapTo(Long.class).one();

            // Without AUTOINCREMENT SQLite re-issues the rowid, and a saved currentTrackId would resume
            // the stored position inside a completely different song.
            assertTrue(secondId > firstId, "a re-used id would resume playback inside the wrong track");
        }
    }

    @Test
    void unscannedTracksAreFoundThroughTheirOwnIndex() {
        try (Handle handle = migratedDatabase()) {
            handle.execute("INSERT INTO jukebox_track (path, ordinal) VALUES ('/music/a.mp3', 0)");
            handle.execute("INSERT INTO jukebox_track (path, ordinal, tagsScannedAt) "
                    + "VALUES ('/music/b.mp3', 1, '2026-08-30T10:00:00Z')");

            // EXPLAIN QUERY PLAN answers id/parent/notused/detail - the human-readable half is "detail".
            String plan = String.join(" ", handle.createQuery(
                            "EXPLAIN QUERY PLAN SELECT * FROM jukebox_track "
                                    + "WHERE tagsScannedAt IS NULL ORDER BY ordinal LIMIT 64")
                    .map((rs, ctx) -> rs.getString("detail")).list());

            assertTrue(plan.contains("idx_jukebox_track_unscanned"),
                    "the tag scanner's query is the reason the partial index exists: " + plan);
        }
    }

    private static Handle migratedDatabase() {
        Handle handle = Jdbi.create("jdbc:sqlite::memory:").open();
        applyMigration(handle);
        return handle;
    }

    private static void applyMigration(Handle handle) {
        for (String statement : loadMigration().split(";")) {
            if (!statement.isBlank()) {
                handle.execute(statement);
            }
        }
    }

    private static String loadMigration() {
        try (InputStream input = JukeboxSchemaTest.class.getResourceAsStream(
                "/db-migration/01100__jukebox.sql")) {
            assertNotNull(input, "migration 01100__jukebox.sql is missing from the classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the jukebox migration", e);
        }
    }
}
