package elite.intel.db.managers;

import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.util.Database;
import elite.intel.gameapi.signals.ConflictZoneProfile;
import elite.intel.gameapi.signals.WarZone;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ledger of where the wars are.
 * <p>
 * What makes it different from the hunting-ground ledger is time: a resource site is a feature of
 * a ring and a war is over in a week. So the cases here are about a sighting going stale, a war
 * ending, and a new war not inheriting the old one's zone count. Each case works in its own corner
 * of the galaxy and searches a one light year radius around it, so the tests share the database
 * without seeing each other's systems.
 */
class ConflictZoneLedgerTest {

    private static final String SEEN = "2026-09-10T12:00:00Z";
    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    private final ConflictZoneManager ledger = ConflictZoneManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void theHardestFightIsOfferedBeforeTheNearest() {
        Coordinates here = at("CZ Ledger Here A", 1000);
        zones("CZ Ledger Low A", 1000.1, new ConflictZoneProfile(5, 0, 0, 0), SEEN);
        zones("CZ Ledger High A", 1000.8, new ConflictZoneProfile(1, 0, 2, 0), SEEN);

        List<String> offered = ledger.bestWarZones(here, 1, NOW).stream().map(WarZone::starSystem).toList();

        assertEquals(List.of("CZ Ledger High A", "CZ Ledger Low A"), offered,
                "within the range the commander asked for, the question is where the real fighting is");
    }

    @Test
    void aWarSightedTodayIsOfferedBeforeOneSightedYesterday() {
        Coordinates here = at("CZ Ledger Here H", 8000);
        zones("CZ Ledger Yesterday H", 8000.1, new ConflictZoneProfile(0, 0, 3, 0), "2026-09-10T23:59:00Z");
        zones("CZ Ledger Today H", 8000.8, new ConflictZoneProfile(2, 0, 0, 0), "2026-09-11T00:10:00Z");
        zones("CZ Ledger Today Too H", 8000.5, new ConflictZoneProfile(0, 0, 1, 0), "2026-09-11T00:05:00Z");

        List<String> offered = ledger.bestWarZones(here, 1, NOW).stream().map(WarZone::starSystem).toList();

        assertEquals(List.of("CZ Ledger Today Too H", "CZ Ledger Today H", "CZ Ledger Yesterday H"), offered,
                "the wars are redrawn weekly, so the one seen most recently is the one most likely still on - "
                        + "and within a day the harder fight still leads, whatever the minute");
    }

    @Test
    void aWarNotSightedForAWeekIsOver() {
        Coordinates here = at("CZ Ledger Here B", 2000);
        zones("CZ Ledger Old B", 2000.1, new ConflictZoneProfile(0, 2, 0, 0), "2026-09-01T00:00:00Z");
        zones("CZ Ledger Fresh B", 2000.5, new ConflictZoneProfile(2, 0, 0, 0), SEEN);

        List<String> offered = ledger.bestWarZones(here, 1, NOW).stream().map(WarZone::starSystem).toList();

        assertEquals(List.of("CZ Ledger Fresh B"), offered, "eleven days without a sighting is a war that has ended");
        assertNull(ledger.zonesIn("CZ Ledger Old B", NOW));
    }

    @Test
    void anArrivalThatFindsNoWarEndsIt() {
        Coordinates here = at("CZ Ledger Here C", 3000);
        zones("CZ Ledger Peace C", 3000.1, new ConflictZoneProfile(0, 0, 3, 0), SEEN);

        ledger.recordPeace("CZ Ledger Peace C", "2026-09-11T00:00:00Z");

        assertTrue(ledger.bestWarZones(here, 1, NOW).isEmpty(),
                "the Conflicts block of a jump listing no active war is the game saying the zones are gone");
    }

    @Test
    void aNewWarDoesNotInheritTheOldWarsZones() {
        Coordinates here = at("CZ Ledger Here D", 4000);
        zones("CZ Ledger Twice D", 4000.1, new ConflictZoneProfile(0, 0, 4, 0), "2026-08-01T00:00:00Z");

        zones("CZ Ledger Twice D", 4000.1, new ConflictZoneProfile(2, 0, 0, 0), SEEN);

        WarZone zone = ledger.bestWarZones(here, 1, NOW).getFirst();
        assertEquals(new ConflictZoneProfile(2, 0, 0, 0), zone.zones(),
                "the sweep landed on a row a month stale, so it replaces the counts rather than max-merging with them");
    }

    @Test
    void withinAWarThePartialSweepNeverLowersTheCount() {
        Coordinates here = at("CZ Ledger Here E", 5000);
        zones("CZ Ledger Partial E", 5000.1, new ConflictZoneProfile(3, 1, 0, 0), SEEN);

        zones("CZ Ledger Partial E", 5000.1, new ConflictZoneProfile(1, 0, 0, 0), "2026-09-11T00:00:00Z");

        assertEquals(new ConflictZoneProfile(3, 1, 0, 0), ledger.bestWarZones(here, 1, NOW).getFirst().zones());
    }

    @Test
    void theSidesJoinTheZonesWhicheverArrivesFirst() {
        Coordinates here = at("CZ Ledger Here F", 6000);
        Coordinates there = at("CZ Ledger Sides F", 6000.1);
        ledger.recordWar("CZ Ledger Sides F", 601L, there, "civilwar", "Sirius Corporation", "RSR", SEEN);

        assertTrue(ledger.bestWarZones(here, 1, NOW).isEmpty(), "the sides alone are not a place to fly to");

        zones("CZ Ledger Sides F", 6000.1, new ConflictZoneProfile(0, 2, 0, 0), SEEN);

        WarZone zone = ledger.bestWarZones(here, 1, NOW).getFirst();
        assertTrue(zone.sidesKnown());
        assertEquals("civilwar", zone.warType());
        assertEquals("Sirius Corporation", zone.faction1());
        assertEquals("RSR", zone.faction2());
    }

    @Test
    void aSystemWithOnlyPowerplayZonesIsNotOffered() {
        Coordinates here = at("CZ Ledger Here G", 7000);
        zones("CZ Ledger Power G", 7000.1, new ConflictZoneProfile(0, 0, 0, 2), SEEN);

        assertTrue(ledger.bestWarZones(here, 1, NOW).isEmpty(), "a power's war pays no faction combat bonds");
    }

    private static Coordinates at(String starSystem, double x) {
        return new Coordinates(starSystem, x, 0, 0);
    }

    private void zones(String starSystem, double x, ConflictZoneProfile profile, String seenAt) {
        ledger.recordZones(starSystem, null, at(starSystem, x), profile, seenAt);
    }
}
