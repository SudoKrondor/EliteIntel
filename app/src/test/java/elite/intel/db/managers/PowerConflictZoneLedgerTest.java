package elite.intel.db.managers;

import elite.intel.db.dao.LocationDao.Coordinates;
import elite.intel.db.util.Database;
import elite.intel.gameapi.signals.PowerWarZone;
import elite.intel.util.Cypher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ledger of where the powers are fighting.
 * <p>
 * What sets it apart from the faction-war ledger is who may ask: a zone is offered only to the power
 * the commander is pledged to, and only once an arrival has said that power is there. The zones and the
 * powers come from different events in either order, so the cases are about them meeting, ageing, and
 * one power never being mistaken for another. Each case works in its own corner of the galaxy.
 */
class PowerConflictZoneLedgerTest {

    private static final String SEEN = "2026-10-24T12:00:00Z";
    private static final Instant NOW = Instant.parse("2026-10-25T20:00:00Z");

    private final PowerConflictZoneManager ledger = PowerConflictZoneManager.getInstance();

    @BeforeAll
    static void boot() throws Exception {
        Cypher.initializeKey();
        Database.init().close();
    }

    @Test
    void theCommandersOwnArrivalComesFirstAndTheSweepJoinsIt() {
        Coordinates here = at("PCZ Here A", 21000);
        arrival("PCZ Hyades A", 81001, 21000.2, null, List.of("A. Lavigny-Duval", "Aisling Duval", "Felicia Winters"),
                "2026-10-25T19:00:00Z", true);
        sweep("PCZ Hyades A", 81001, 21000.2, 3, "2026-10-25T19:00:20Z");

        PowerWarZone zone = only(ledger.bestZonesFor("Felicia Winters", here, 1, NOW));

        assertEquals("PCZ Hyades A", zone.starSystem());
        assertEquals(3, zone.zones());
        assertNull(zone.controllingPower(), "an unoccupied system is held by nobody");
        assertEquals(List.of("A. Lavigny-Duval", "Aisling Duval", "Felicia Winters"), zone.contenders());
    }

    @Test
    void zonesWhosePowersNoArrivalHasNamedAreNotOffered() {
        Coordinates here = at("PCZ Here B", 22000);
        sweep("PCZ Unnamed B", 81002, 22000.2, 2, SEEN);

        assertTrue(ledger.bestZonesFor("Zemina Torval", here, 1, NOW).isEmpty(),
                "nothing says the commander's power is in that fight");
    }

    @Test
    void onePowersNameNeverMatchesInsideAnothers() {
        Coordinates here = at("PCZ Here C", 23000);
        arrival("PCZ Duval C", 81003, 23000.2, "A. Lavigny-Duval", List.of("A. Lavigny-Duval"), SEEN, true);
        sweep("PCZ Duval C", 81003, 23000.2, 1, SEEN);

        assertTrue(ledger.bestZonesFor("Duval", here, 1, NOW).isEmpty());
        assertEquals(1, ledger.bestZonesFor("A. Lavigny-Duval", here, 1, NOW).size());
    }

    @Test
    void zonesNotSightedForAWeekAreOverAndTheNextCycleStartsFromZero() {
        Coordinates here = at("PCZ Here D", 24000);
        arrival("PCZ Old D", 81004, 24000.2, "Yuri Grom", List.of("Yuri Grom", "Denton Patreus"), SEEN, true);
        sweep("PCZ Old D", 81004, 24000.2, 11, "2026-10-10T00:00:00Z");

        assertTrue(ledger.bestZonesFor("Denton Patreus", here, 1, NOW).isEmpty(), "two weeks unseen is a cycle ago");

        sweep("PCZ Old D", 81004, 24000.2, 2, SEEN);
        assertEquals(2, only(ledger.bestZonesFor("Denton Patreus", here, 1, NOW)).zones(),
                "the old cycle's eleven zones never max-merge into this one's two");
    }

    @Test
    void theMostZonesLeadWithinADayThenTheNearest() {
        Coordinates here = at("PCZ Here E", 25000);
        for (Object[] system : new Object[][]{{"PCZ Few E", 81005L, 25000.1, 1}, {"PCZ Many E", 81006L, 25000.8, 6},
                {"PCZ Few Far E", 81007L, 25000.9, 1}}) {
            arrival((String) system[0], (Long) system[1], (Double) system[2], "Archon Delaine",
                    List.of("Archon Delaine", "Nakato Kaine"), SEEN, true);
            sweep((String) system[0], (Long) system[1], (Double) system[2], (Integer) system[3], SEEN);
        }

        List<String> offered = ledger.bestZonesFor("Nakato Kaine", here, 1, NOW).stream()
                .map(PowerWarZone::starSystem).toList();

        assertEquals(List.of("PCZ Many E", "PCZ Few E", "PCZ Few Far E"), offered);
    }

    @Test
    void theRelayNeverCreatesARowFromAnArrivalAlone() {
        Coordinates here = at("PCZ Here F", 26000);
        arrival("PCZ Relay F", 81008, 26000.2, "Pranav Antal", List.of("Pranav Antal"), SEEN, false);
        sweep("PCZ Relay F", 81008, 26000.2, 2, "2026-10-24T13:00:00Z");

        assertTrue(ledger.bestZonesFor("Pranav Antal", here, 1, NOW).isEmpty(),
                "the relay's arrivals only update systems already known to hold zones");

        arrival("PCZ Relay F", 81008, 26000.2, "Pranav Antal", List.of("Pranav Antal"), "2026-10-24T14:00:00Z", false);
        assertEquals(1, ledger.bestZonesFor("Pranav Antal", here, 1, NOW).size());
    }

    private void sweep(String starSystem, long address, double x, int zones, String seenAt) {
        ledger.recordZones(starSystem, address, new Coordinates(starSystem, x, 0, 0), zones, seenAt);
    }

    private void arrival(String starSystem, long address, double x, String controllingPower, List<String> powers,
                         String seenAt, boolean createIfMissing) {
        ledger.recordArrival(starSystem, address, new Coordinates(starSystem, x, 0, 0), controllingPower, powers,
                null, seenAt, createIfMissing);
    }

    private static PowerWarZone only(List<PowerWarZone> zones) {
        assertEquals(1, zones.size(), zones.toString());
        return zones.getFirst();
    }

    private static Coordinates at(String name, double x) {
        return new Coordinates(name, x, 0, 0);
    }
}
