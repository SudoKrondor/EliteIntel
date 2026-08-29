package elite.intel.gameapi.signals;

import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Collapsing one system's signals across the body records they are scattered over.
 */
class SystemSignalsTest {

    private static final long HYADES_MH_V_C2_8 = 2283077046962L;

    private static FssSignalDto signal(String name, String type) {
        FssSignalDto signal = new FssSignalDto();
        signal.setSignalName(name);
        signal.setSignalType(type);
        signal.setSystemAddress(HYADES_MH_V_C2_8);
        return signal;
    }

    private static LocationDto body(long bodyId, String planetName, FssSignalDto... signals) {
        LocationDto location = new LocationDto(bodyId, HYADES_MH_V_C2_8);
        location.setPlanetName(planetName);
        for (FssSignalDto signal : signals) {
            location.addDetectedSignal(signal);
        }
        return location;
    }

    /**
     * The case this came from: one carrier, honked on arrival and again after moving in-system, filed against
     * two different body records and reported to the commander as two carriers.
     */
    @Test
    @DisplayName("one carrier filed against two body records is one carrier")
    void collapsesTheSameSignalAcrossRecords() {
        List<SystemSignals.Sighting> distinct = SystemSignals.distinct(List.of(
                body(0L, "Hyades Sector MH-V c2-8 A", signal("LONE WOLF GHY-L8X", "FleetCarrier")),
                body(6L, "Hyades Sector MH-V c2-8 1", signal("LONE WOLF GHY-L8X", "FleetCarrier"))));

        assertEquals(1, distinct.size());
        assertEquals("LONE WOLF GHY-L8X", distinct.getFirst().signal().getSignalName());
    }

    @Test
    @DisplayName("two genuinely different carriers stay two")
    void keepsDistinctSignalsApart() {
        List<SystemSignals.Sighting> distinct = SystemSignals.distinct(List.of(
                body(0L, "A", signal("LONE WOLF GHY-L8X", "FleetCarrier")),
                body(6L, "1", signal("K7Q-BQL", "FleetCarrier"))));

        assertEquals(2, distinct.size());
    }

    /**
     * Body signals are filed under the event's name, so two bodies both reporting biological signals produce
     * records equal in every field. They are two findings on two worlds, and collapsing them would cost the
     * commander a planet worth landing on.
     */
    @Test
    @DisplayName("identical body-signal records on two bodies stay two")
    void keepsBodyScopedRecordsPerBody() {
        List<SystemSignals.Sighting> distinct = SystemSignals.distinct(List.of(
                body(6L, "Hyades Sector MH-V c2-8 1", signal("FSSBodySignals", "Biological")),
                body(7L, "Hyades Sector MH-V c2-8 2", signal("FSSBodySignals", "Biological"))));

        assertEquals(2, distinct.size());
        assertEquals(List.of("Hyades Sector MH-V c2-8 1", "Hyades Sector MH-V c2-8 2"),
                distinct.stream().map(SystemSignals.Sighting::recordedAgainst).toList());
    }

    @Test
    @DisplayName("the first record met supplies the provenance")
    void keepsTheFirstRecordsProvenance() {
        List<SystemSignals.Sighting> distinct = SystemSignals.distinct(List.of(
                body(0L, "Hyades Sector MH-V c2-8 A", signal("Boming Beacon", "Outpost")),
                body(6L, "Hyades Sector MH-V c2-8 1", signal("Boming Beacon", "Outpost"))));

        assertEquals(1, distinct.size());
        assertEquals("Hyades Sector MH-V c2-8 A", distinct.getFirst().recordedAgainst());
    }

    @Test
    @DisplayName("signals from every record of the system are reported, in the order met")
    void readsEveryRecordOfTheSystem() {
        List<SystemSignals.Sighting> distinct = SystemSignals.distinct(List.of(
                body(0L, "A", signal("Boming Beacon", "Outpost")),
                body(6L, "1", signal("Okuni Terminal", "Installation"))));

        assertEquals(List.of("Boming Beacon", "Okuni Terminal"),
                distinct.stream().map(sighting -> sighting.signal().getSignalName()).toList());
    }

    @Test
    @DisplayName("no records, a null list, and a record with no signals are all simply empty")
    void toleratesNothingToRead() {
        assertTrue(SystemSignals.distinct(null).isEmpty());
        assertTrue(SystemSignals.distinct(List.of()).isEmpty());
        assertTrue(SystemSignals.distinct(List.of(body(0L, "A"))).isEmpty());
    }
}
