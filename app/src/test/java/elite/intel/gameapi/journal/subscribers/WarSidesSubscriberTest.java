package elite.intel.gameapi.journal.subscribers;

import com.google.gson.reflect.TypeToken;
import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.managers.ConflictZoneManager;
import elite.intel.db.util.Database;
import elite.intel.gameapi.journal.events.FSDJumpEvent;
import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.WarZone;
import elite.intel.util.Cypher;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An arrival's Conflicts block reaching the conflict-zone ledger: the sides of a war join the zones a sweep
 * already counted, and an arrival that finds no war ends them. Each case has its own system so the shared
 * database keeps the tests apart.
 */
class WarSidesSubscriberTest {

    // Relative to now: the ledger forgets a war unseen for WAR_LIFETIME, and a fixed date would age past
    // it and start failing on the day it turned a week old.
    private static final String SEEN = Instant.now().minus(Duration.ofHours(2)).toString();
    private static final String LATER = Instant.now().minus(Duration.ofHours(1)).toString();

    private final ConflictZoneManager ledger = ConflictZoneManager.getInstance();
    private final WarSidesSubscriber subscriber = new WarSidesSubscriber(ledger);

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void anArrivalNamesTheSidesOfTheWarTheSweepCounted() {
        String system = "War Sides Test A";
        ledger.recordZones(system, 1L, new Coordinates(system, 9000, 0, 0), new ConflictZoneProfile(0, 2, 0, 0), SEEN);

        subscriber.record(system, 1L, new double[]{9000, 0, 0}, conflicts("""
                [{"WarType": "civilwar", "Status": "active", "Faction1": {"Name": "Sirius Corporation"}, "Faction2": {"Name": "RSR"}}]
                """), LATER);

        WarZone zone = ledger.zonesIn(system);
        assertNotNull(zone);
        assertTrue(zone.sidesKnown());
        assertEquals("civilwar", zone.warType());
        assertEquals("Sirius Corporation", zone.faction1());
        assertEquals("RSR", zone.faction2());
    }

    @Test
    void anArrivalThatFindsNoWarEndsTheZones() {
        String system = "War Sides Test B";
        ledger.recordZones(system, 2L, new Coordinates(system, 9100, 0, 0), new ConflictZoneProfile(3, 0, 0, 0), SEEN);

        subscriber.record(system, 2L, new double[]{9100, 0, 0}, List.of(), LATER);

        assertNull(ledger.zonesIn(system), "no Conflicts entry is the game saying the war is over");
    }

    @Test
    void anArrivalWithNoSystemNameIsIgnored() {
        assertDoesNotThrow(() -> subscriber.record(null, 3L, null, null, LATER));
    }

    private static List<FSDJumpEvent.Conflict> conflicts(String json) {
        return GsonFactory.getGson().fromJson(json, new TypeToken<List<FSDJumpEvent.Conflict>>() {
        }.getType());
    }
}
