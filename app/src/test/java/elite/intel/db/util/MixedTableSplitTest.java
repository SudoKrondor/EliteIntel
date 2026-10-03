package elite.intel.db.util;

import com.google.gson.JsonObject;
import elite.intel.util.json.GsonFactory;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The commander's half of the four tables that stay shared, moved out of a v1.1.0019 database.
 * <p>
 * Each table is a galaxy fact every commander shares with one or two columns about what this commander did. After
 * the move the fact stays shared and the "what I did" is in the commander's own file, so the next commander finds
 * the place without finding it already discovered, sampled or forgotten.
 */
class MixedTableSplitTest {

    private static final String PRETTY_ROW = """
            {
              "locationType": "STATION",
              "starName": "Sol",
              "ourDiscovery": true,
              "weMappedIt": false,
              "bioScansCompleted": true,
              "partialBioSamples": [
                "Bacterium"
              ],
              "isHomeSystem": false
            }""";

    @Test
    void theCommandersHalvesMoveAndTheSharedColumnsGo() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.fresh()) {
            Handle h = built.handle;
            CommanderSplit.runIfNeeded(h);
            h.execute("UPDATE main.material_names SET amount = 7 WHERE symbol = 'iron'");
            h.execute("INSERT INTO main.hunting_ground (starSystem, forgotten) VALUES ('Sol', 1), ('Lave', 0)");
            h.execute("INSERT INTO main.exo_mastery_body (systemAddress, bodyId, starSystem, bodyName, value, completed, completedAt) "
                    + "VALUES (1, 2, 'Sol', 'Sol 2', 900, 1, '2026-09-01'), (1, 3, 'Sol', 'Sol 3', 50, 0, NULL)");
            h.execute("INSERT INTO main.exo_mastery_species (systemAddress, bodyId, speciesSymbol, speciesName, sampled) "
                    + "VALUES (1, 2, 'bact', 'Bacterium', 1), (1, 3, 'fung', 'Fungoida', 0)");
            h.execute("INSERT INTO main.location (inGameId, locationName, primaryStar, json, systemAddress) "
                    + "VALUES (5, 'Abraham Lincoln', 'Sol', ?, 1)", PRETTY_ROW);

            CommanderSplit.splitMixedIfNeeded(h);

            assertEquals(7, one(h, "SELECT amount FROM cmdr.material_inventory WHERE symbol = 'iron'"));
            assertEquals(1, one(h, "SELECT COUNT(*) FROM cmdr.hunting_ground_forgotten WHERE starSystem = 'Sol'"));
            assertEquals(900, one(h, "SELECT value FROM cmdr.exo_mastery_harvest WHERE bodyId = 2"));
            assertEquals(1, one(h, "SELECT COUNT(*) FROM cmdr.exo_mastery_harvest"), "only the completed body");
            assertEquals(1, one(h, "SELECT COUNT(*) FROM cmdr.exo_mastery_sample WHERE speciesSymbol = 'bact'"));
            for (String[] gone : new String[][]{{"material_names", "amount"}, {"hunting_ground", "forgotten"},
                    {"exo_mastery_body", "completed"}, {"exo_mastery_body", "completedAt"}, {"exo_mastery_species", "sampled"}}) {
                assertEquals(0, one(h, "SELECT COUNT(*) FROM pragma_table_info('" + gone[0] + "', 'main') WHERE name = '" + gone[1] + "'"),
                        gone[0] + "." + gone[1] + " is gone from the shared file");
            }

            String shared = h.createQuery("SELECT json FROM main.location WHERE locationName = 'Abraham Lincoln'")
                    .mapTo(String.class).one();
            JsonObject sharedRow = GsonFactory.getGson().fromJson(shared, JsonObject.class);
            assertFalse(sharedRow.get("ourDiscovery").getAsBoolean(), "the next commander did not discover it");
            assertTrue(sharedRow.get("partialBioSamples").getAsJsonArray().isEmpty());
            assertTrue(shared.contains("\"locationType\": \"STATION\""), "the shared row keeps Gson's own format");
            String flags = h.createQuery("SELECT flags FROM cmdr.location_visit WHERE locationName = 'Abraham Lincoln'")
                    .mapTo(String.class).one();
            JsonObject merged = GsonFactory.getGson().fromJson(LocationFlags.merge(shared, flags), JsonObject.class);
            assertTrue(merged.get("ourDiscovery").getAsBoolean(), "this commander still discovered it");
            assertTrue(merged.get("bioScansCompleted").getAsBoolean());
            assertEquals("Bacterium", merged.get("partialBioSamples").getAsJsonArray().get(0).getAsString());
        }
    }

    @Test
    void runningTwiceChangesNothing() throws Exception {
        try (MigrationLayoutTest.Built built = MigrationLayoutTest.Built.freshAndSplit()) {
            Handle h = built.handle;
            h.execute("INSERT INTO cmdr.material_inventory (symbol, amount) VALUES ('iron', 3)");
            CommanderSplit.splitMixedIfNeeded(h);
            assertEquals(3, one(h, "SELECT amount FROM cmdr.material_inventory WHERE symbol = 'iron'"));
        }
    }

    @Test
    void aLocationTheCommanderDidNothingAtHasNoFlags() {
        LocationFlags.Split split = LocationFlags.split("""
                {
                  "starName": "Sol",
                  "ourDiscovery": false,
                  "partialBioSamples": []
                }""");
        assertNull(split.flags());
    }

    @Test
    void splittingAndMergingGiveBackTheSameRow() {
        LocationFlags.Split split = LocationFlags.split(PRETTY_ROW);
        assertEquals(PRETTY_ROW, LocationFlags.merge(split.sharedJson(), split.flags()));
    }

    private static int one(Handle h, String sql) {
        return h.createQuery(sql).mapTo(Integer.class).one();
    }
}
