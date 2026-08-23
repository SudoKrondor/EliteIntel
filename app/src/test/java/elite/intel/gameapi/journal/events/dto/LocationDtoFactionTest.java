package elite.intel.gameapi.journal.events.dto;

import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fleet carrier has no faction, and the journal says so by writing the word "FleetCarrier" where a faction
 * name goes.
 *
 * <p>Taken literally it came back out of the app as "the controlling faction there is FleetCarrier" - a
 * faction that does not exist, stated as fact, about a system whose real controlling faction we had simply
 * never recorded. Absent is the truth, and every reader of these fields already handles absent.
 */
class LocationDtoFactionTest {

    @Test
    void aCarrierIsNotAFaction() {
        LocationDto location = new LocationDto(1L, 2L);
        location.setStationFaction("FleetCarrier");

        assertNull(location.getStationFaction());
    }

    @Test
    void aRealFactionIsKeptExactlyAsTheJournalSpellsIt() {
        LocationDto location = new LocationDto(1L, 2L);
        location.setStationFaction("HIP 23421 Worker's Party");

        assertEquals("HIP 23421 Worker's Party", location.getStationFaction());
    }

    @Test
    void aBlankFactionReadsAsAbsentRatherThanAsAnEmptyName() {
        LocationDto location = new LocationDto(1L, 2L);
        location.setStationFaction("   ");

        assertNull(location.getStationFaction());
    }

    /**
     * The rule has to survive a round trip through the database, because rows written before it existed are
     * still there holding the sentinel. Locations are persisted as JSON, so this is what a stored row does.
     */
    @Test
    void aRowStoredBeforeThisRuleExistedStillReadsAsAbsent() {
        LocationDto stored = GsonFactory.getGson().fromJson(
                "{\"stationFaction\":\"FleetCarrier\",\"systemFaction\":\"FleetCarrier\"}", LocationDto.class);

        assertNull(stored.getStationFaction());
        assertNull(stored.getSystemFaction());
    }

    /**
     * The system's controlling faction and the station's operator are two different questions. Conflating
     * them is how a carrier's sentinel came to be spoken as the system's controlling faction in the first
     * place.
     */
    @Test
    void theSystemFactionIsHeldSeparatelyFromTheStationsOwn() {
        LocationDto location = new LocationDto(1L, 2L);
        location.setSystemFaction("Brazilian League of Pilots");
        location.setStationFaction("Brewer Corporation");

        assertEquals("Brazilian League of Pilots", location.getSystemFaction());
        assertEquals("Brewer Corporation", location.getStationFaction());
    }
}
