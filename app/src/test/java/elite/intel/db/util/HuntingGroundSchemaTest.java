package elite.intel.db.util;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the upgrade from the crowd-sourced tables has to preserve and what it has to remove.
 * <p>
 * The rows worth carrying across are the ones the commander confirmed by hand: under the old workflow
 * they flew to a system, saw its rings and said so, and that is a real observation. The rest of the old
 * data was a service's guesses, and the journal scan recovers the genuine pairs from real accepted
 * contracts rather than inheriting them.
 */
class HuntingGroundSchemaTest {

    @Test
    void handConfirmedHuntingGroundsSurviveTheUpgrade() {
        try (Handle handle = upgradedDatabase()) {
            assertEquals(1, handle.createQuery(
                            "SELECT COUNT(*) FROM hunting_ground WHERE starSystem = 'Odomazotz'")
                    .mapTo(Integer.class).one());
            assertEquals(0.0, handle.createQuery("SELECT x FROM hunting_ground WHERE starSystem = 'Odomazotz'")
                    .mapTo(Double.class).one(), 0.001);
            assertEquals(0, handle.createQuery(
                                    "SELECT resHazardous FROM hunting_ground WHERE starSystem = 'Odomazotz'")
                            .mapTo(Integer.class).one(),
                    "the old workflow recorded that a system had sites, never which grades - the counts "
                            + "stay at zero until it is flown to again");
        }
    }

    @Test
    void aSystemTheCommanderNeverConfirmedIsNotCarriedAcross() {
        try (Handle handle = upgradedDatabase()) {
            assertEquals(0, handle.createQuery(
                                    "SELECT COUNT(*) FROM hunting_ground WHERE starSystem = 'Unverified Guess'")
                            .mapTo(Integer.class).one(),
                    "a target the service proposed and nobody ever checked is not an observation");
        }
    }

    @Test
    void aGroundTheCommanderHadAlreadyRejectedStaysRejected() {
        try (Handle handle = upgradedDatabase()) {
            assertEquals(1, handle.createQuery(
                                    "SELECT forgotten FROM hunting_ground WHERE starSystem = 'Rejected System'")
                            .mapTo(Integer.class).one(),
                    "the old ignore flag was the same verdict under a different name");
        }
    }

    @Test
    void theCrowdSourcedTablesAreGone() {
        try (Handle handle = upgradedDatabase()) {
            assertEquals(0, tableCount(handle, "pirate_hunting_grounds"));
            assertEquals(0, tableCount(handle, "mission_provider"));
        }
    }

    @Test
    void theWorkflowTheHelpDescribesIsTheOneTheAppNowHas() {
        try (Handle handle = upgradedDatabase()) {
            String help = handle.createQuery("SELECT help FROM help_topics WHERE id = 9")
                    .mapTo(String.class).one();

            assertFalse(help.contains("reconnaissance"),
                    "recon and manual confirmation are gone - sites announce themselves on arrival");
            assertTrue(help.contains("Scan journals for hunting grounds"));
            assertTrue(help.contains("Find a bounty hunting ground"));
        }
    }

    @Test
    void theScanBookmarkStartsEmptySoTheFirstRunReadsEverything() {
        try (Handle handle = upgradedDatabase()) {
            assertNull(handle.createQuery("SELECT lastJournal FROM hunting_ground_scan WHERE id = 1")
                    .mapTo(String.class).one());
        }
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * A database holding what the crowd-sourced feature left behind, with the migration applied on top.
     */
    private static Handle upgradedDatabase() {
        Handle handle = Jdbi.create("jdbc:sqlite::memory:").open();
        handle.execute("""
                CREATE TABLE pirate_hunting_grounds (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    starSystem    TEXT             NOT NULL UNIQUE,
                    x             DOUBLE PRECISION NOT NULL,
                    y             DOUBLE PRECISION NOT NULL,
                    z             DOUBLE PRECISION NOT NULL,
                    hasResSite    BOOLEAN          NOT NULL DEFAULT FALSE,
                    ignored       BOOLEAN          NOT NULL DEFAULT FALSE,
                    targetFaction TEXT)
                """);
        handle.execute("""
                CREATE TABLE mission_provider (
                    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                    starSystem             TEXT             NOT NULL,
                    targetSystem           TEXT,
                    x                      DOUBLE PRECISION NOT NULL,
                    y                      DOUBLE PRECISION NOT NULL,
                    z                      DOUBLE PRECISION NOT NULL,
                    missionProviderFaction TEXT,
                    targetFactionID        INTEGER)
                """);
        handle.execute("CREATE TABLE help_topics (id INTEGER PRIMARY KEY, topic TEXT, help TEXT)");

        handle.execute("INSERT INTO help_topics VALUES (9, 'Pirate Massacre Missions', "
                + "'Old text about reconnaissance and manual confirmation.')");
        handle.execute("INSERT INTO pirate_hunting_grounds (starSystem, x, y, z, hasResSite, ignored) "
                + "VALUES ('Odomazotz', 0, 0, 0, 1, 0)");
        handle.execute("INSERT INTO pirate_hunting_grounds (starSystem, x, y, z, hasResSite, ignored) "
                + "VALUES ('Unverified Guess', 5, 5, 5, 0, 0)");
        handle.execute("INSERT INTO pirate_hunting_grounds (starSystem, x, y, z, hasResSite, ignored) "
                + "VALUES ('Rejected System', 9, 9, 9, 1, 1)");

        applyMigration(handle);
        return handle;
    }

    private static int tableCount(Handle handle, String table) {
        return handle.createQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = :name")
                .bind("name", table).mapTo(Integer.class).one();
    }

    private static void applyMigration(Handle handle) {
        for (String statement : loadMigration().split(";\\s*\n")) {
            if (!statement.isBlank()) {
                handle.execute(statement);
            }
        }
    }

    private static String loadMigration() {
        try (InputStream input = HuntingGroundSchemaTest.class
                .getResourceAsStream("/db-migration/01048__local_hunting_grounds.sql")) {
            assertNotNull(input, "the migration must be on the test classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
