package elite.intel.junit.db;

import elite.intel.db.util.Database;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration 01026 clears first-discovery claims a nav beacon invented.
 * <p>
 * A beacon scan reports {@code WasDiscovered:false} for bodies charted decades ago, which was stored as
 * {@code ourDiscovery=true}. That credited the commander with discovering bodies in long-settled systems and
 * inflated the exobiology first-discovery bonus, which reads the same field.
 * <p>
 * The migration has already run against the test database by the time these execute, so each case seeds its own
 * rows and re-runs the statement: what is under test is the rule, not the one-off execution.
 */
class NavBeaconDiscoveryFlagMigrationTest {

    /**
     * Verbatim from 01026.
     */
    private static final String MIGRATION = """
            UPDATE location
               SET json = json_set(json, '$.ourDiscovery', json('false'))
             WHERE json_extract(json, '$.ourDiscovery') = 1
               AND primaryStar IN (
                   SELECT primaryStar
                     FROM location
                    WHERE COALESCE(json_extract(json, '$.population'), 0) > 0)
            """;

    private static final String INHABITED = "Wolf 1323";
    private static final String EMPTY_SPACE = "Pleiades Sector QO-Q b5-3";

    /**
     * Every inhabited-system case needs the populated primary-star row present: population is what marks the
     * system, and the migration reads it from a sibling row. The app records it on arrival (FSDJump), so this
     * mirrors a real database rather than propping the test up.
     */
    @BeforeEach
    void seedInhabitedSystem() {
        seed(INHABITED, "Wolf 1323", true, true, 152000L);
    }

    @AfterEach
    void removeSeededRows() {
        withDb(h -> h.createUpdate("DELETE FROM location WHERE primaryStar IN (:a, :b)")
                .bind("a", INHABITED).bind("b", EMPTY_SPACE).execute());
    }

    @Test
    void aFirstDiscoveryInAnInhabitedSystemIsCleared() {
        seed(INHABITED, "Karpo", true, false, 0L);

        runMigration();

        assertFalse(ourDiscovery("Wolf 1323"), "nobody discovers the star of a populated system");
        assertFalse(ourDiscovery("Karpo"), "nor its planets");
    }

    /**
     * The journal's self-contradiction: discovered by us, yet already mapped by somebody else. Mapping requires
     * a prior discovery, so this combination cannot occur and is the clearest fingerprint of a beacon scan.
     */
    @Test
    void theImpossibleCombinationIsCleared() {
        seed(INHABITED, "Rattus", true, false, 0L);

        runMigration();

        assertFalse(ourDiscovery("Rattus"));
    }

    @Test
    void aGenuineDiscoveryInEmptySpaceIsUntouched() {
        seed(EMPTY_SPACE, "Pleiades Sector QO-Q b5-3", true, true, 0L);

        runMigration();

        assertTrue(ourDiscovery("Pleiades Sector QO-Q b5-3"), "an uninhabited system says nothing against us");
    }

    /**
     * Mapping a body yourself in an inhabited system is ordinary and pays out, so unlike a first discovery it
     * is not self-contradictory and must survive.
     */
    @Test
    void mappingCreditInAnInhabitedSystemSurvives() {
        seed(INHABITED, "Wolf 1323 3", true, true, 0L);

        runMigration();

        assertFalse(ourDiscovery("Wolf 1323 3"));
        assertTrue(weMappedIt("Wolf 1323 3"), "we really can map a body in a populated system");
    }

    @Test
    void theFlagStaysARealBooleanRatherThanANumber() {
        seed(INHABITED, "Wolf 1323 4", true, true, 0L);

        runMigration();

        // json_set with 0 would store a number where the DTO declares a boolean, so the raw text is checked.
        assertTrue(json("Wolf 1323 4").contains("\"ourDiscovery\":false"),
                "expected a JSON boolean, got: " + json("Wolf 1323 4"));
    }

    @Test
    void runningItTwiceChangesNothingFurther() {
        seed(INHABITED, "Wolf 1323 5", true, true, 0L);

        runMigration();
        String once = json("Wolf 1323 5");
        runMigration();

        assertEquals(once, json("Wolf 1323 5"));
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static void seed(String system, String body, boolean ourDiscovery, boolean weMappedIt, long population) {
        String json = "{\"starName\":\"" + system + "\",\"planetName\":\"" + body + "\""
                + ",\"ourDiscovery\":" + ourDiscovery
                + ",\"weMappedIt\":" + weMappedIt
                + ",\"population\":" + population + "}";
        withDb(h -> h.createUpdate("""
                        INSERT INTO location (inGameId, locationName, primaryStar, json)
                        VALUES (0, :name, :star, :json)
                        """)
                .bind("name", body).bind("star", system).bind("json", json).execute());
    }

    private static void runMigration() {
        withDb(h -> h.createUpdate(MIGRATION).execute());
    }

    private static boolean ourDiscovery(String body) {
        return flag(body, "$.ourDiscovery");
    }

    private static boolean weMappedIt(String body) {
        return flag(body, "$.weMappedIt");
    }

    private static boolean flag(String body, String path) {
        List<Integer> values = withDb(h -> h.createQuery(
                        "SELECT json_extract(json, '" + path + "') FROM location WHERE locationName = :name")
                .bind("name", body).mapTo(Integer.class).list());
        assertEquals(1, values.size(), body + " should be seeded exactly once");
        return values.getFirst() == 1;
    }

    private static String json(String body) {
        return withDb(h -> h.createQuery("SELECT json FROM location WHERE locationName = :name")
                .bind("name", body).mapTo(String.class).one());
    }

    private static <T> T withDb(java.util.function.Function<Handle, T> block) {
        try (Handle handle = Database.init()) {
            return block.apply(handle);
        }
    }
}
