package elite.intel.db.managers;

import elite.intel.db.util.Database;
import elite.intel.gameapi.cartography.CartographicBody;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ledger follows the data where the game keeps it - in the ship - through scans, honks, mappings, sales
 * and deaths.
 */
class CartographicDataManagerTest {

    private static final AtomicLong SHIPS = new AtomicLong(9_000);
    private static final long SYSTEM = 7_000_001L;
    private static final long OTHER_SYSTEM = 7_000_002L;
    private static final String SYSTEM_NAME = "Ledger Test";
    private static final String OTHER_SYSTEM_NAME = "Ledger Other";

    private final CartographicDataManager ledger = CartographicDataManager.getInstance();
    private long explorer;
    private long fighter;

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @BeforeEach
    void ships() {
        explorer = SHIPS.incrementAndGet();
        fighter = SHIPS.incrementAndGet();
        ledger.boardShip(explorer);
    }

    @AfterEach
    void scrap() {
        for (long ship : List.of(explorer, fighter)) {
            ledger.boardShip(ship);
            ledger.lost();
        }
    }

    @Test
    void aScannedSystemIsWorthItsBodies() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, star());
        ledger.recordBody(SYSTEM, SYSTEM_NAME, icyBody("Ledger Test 1"));

        assertEquals(new CartographicDataManager.Worth(1_213 + 500, 1_213 + 500), ledger.worth(SYSTEM));
    }

    @Test
    void theHonkCountsWhatWasNeverScanned() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, star());
        ledger.recordHonk(SYSTEM, SYSTEM_NAME, 4);

        assertEquals(1_213 + 3 * 500, ledger.worth(SYSTEM).system());
    }

    @Test
    void aRescanKeepsTheMapping() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, icyBody("Ledger Test 1"));
        long scanned = ledger.worth(SYSTEM).system();
        ledger.recordMapping("Ledger Test 1", true);
        long mapped = ledger.worth(SYSTEM).system();

        ledger.recordBody(SYSTEM, SYSTEM_NAME, icyBody("Ledger Test 1"));

        assertEquals(mapped, ledger.worth(SYSTEM).system());
        assertTrue(mapped > scanned);
    }

    @Test
    void aSaleTakesOnlyTheSystemsItNames() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, star());
        ledger.recordHonk(SYSTEM, SYSTEM_NAME, 4);
        ledger.recordBody(OTHER_SYSTEM, OTHER_SYSTEM_NAME, starNamed("Ledger Other"));

        ledger.sold(List.of(SYSTEM_NAME));

        assertEquals(new CartographicDataManager.Worth(0, 1_213), ledger.worth(SYSTEM));
    }

    @Test
    void aDeathLosesOnlyTheDataInTheShipThatDied() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, star());
        ledger.boardShip(fighter);
        ledger.recordBody(OTHER_SYSTEM, OTHER_SYSTEM_NAME, starNamed("Ledger Other"));

        ledger.lost();

        assertEquals(0, ledger.worth(OTHER_SYSTEM).unsold());
        ledger.boardShip(explorer);
        assertEquals(1_213, ledger.worth(SYSTEM).unsold(), "the explorer's data was never in the fighter");
    }

    @Test
    void theShipFlownIsTheOnlyOneCounted() {
        ledger.recordBody(SYSTEM, SYSTEM_NAME, star());

        ledger.boardShip(fighter);

        assertEquals(0, ledger.worth(SYSTEM).unsold());
    }

    @Test
    void finishingTheFssOfAFirstDiscoveryPaysPerBody() {
        CartographicBody ours = new CartographicBody("Ledger Test", true, "K", 0.7, null, false, 0,
                false, false, false, false);
        ledger.recordBody(SYSTEM, SYSTEM_NAME, ours);
        ledger.recordHonk(SYSTEM, SYSTEM_NAME, 1);
        long surveying = ledger.worth(SYSTEM).system();

        ledger.recordAllBodiesFound(SYSTEM);

        assertEquals(surveying + 1_000, ledger.worth(SYSTEM).system());
    }

    // -- fixtures --------------------------------------------------------------------------------------------

    private static CartographicBody star() {
        return starNamed(SYSTEM_NAME);
    }

    private static CartographicBody starNamed(String name) {
        return new CartographicBody(name, true, "K", 0.7, null, false, 0, true, false, false, false);
    }

    private static CartographicBody icyBody(String name) {
        return new CartographicBody(name, false, null, 0, "Icy body", false, 0.02, true, true, false, false);
    }
}
