package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.gameapi.journal.events.dto.FssSignalDto;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemSignalsFactSourceTest {

    private static FssSignalDto signal(String name, String localised, String type) {
        FssSignalDto signal = new FssSignalDto();
        signal.setSignalName(name);
        signal.setSignalNameLocalised(localised);
        signal.setSignalType(type);
        return signal;
    }

    private static LocationDto location(FssSignalDto... signals) {
        LocationDto location = new LocationDto(1L, 100L);
        for (FssSignalDto signal : signals) {
            location.addDetectedSignal(signal);
        }
        return location;
    }

    @Test
    void countsRepeatedSignalsAndReadsEveryBodyRecordOfTheSystem() {
        Map<String, Integer> counts = SystemSignalsFactSource.counted(List.of(
                location(signal("$MULTIPLAYER_SCENARIO14_TITLE;", "Resource Extraction Site [High]", "ResourceExtraction"),
                        signal("$MULTIPLAYER_SCENARIO14_TITLE_2;", "Resource Extraction Site [High]", "ResourceExtraction")),
                location(signal("Hutton Orbital", null, "Station"))));

        assertEquals(2, counts.get("Resource Extraction Site [High]"));
        assertEquals(1, counts.get("Hutton Orbital"));
    }

    /**
     * The carriers are the one entry reported as a bare number, so a signal filed against two body records shows
     * up as a second carrier. A callsign is unique galaxy-wide - measured live, one carrier alone in Hyades
     * Sector MH-V c2-8 was reported as two.
     */
    @Test
    void oneCarrierFiledAgainstTwoBodyRecordsIsStillOneCarrier() {
        Map<String, Integer> counts = SystemSignalsFactSource.counted(List.of(
                location(signal("LONE WOLF GHY-L8X", null, "FleetCarrier")),
                location(signal("LONE WOLF GHY-L8X", null, "FleetCarrier"))));

        assertEquals(1, counts.get("fleet carrier"));
    }

    /**
     * A stored USS carries only a countdown and no record of when it was seen, so it can never be shown to still
     * be there - reporting one would send the commander to a signal that despawned hours ago.
     */
    @Test
    void leavesOutTransientSignalSources() {
        Map<String, Integer> counts = SystemSignalsFactSource.counted(List.of(
                location(signal("$USS_HighGradeEmissions;", "High Grade Emissions", "USS"),
                        signal("$Warzone_TG;", "AX Conflict Zone", "Combat"))));

        assertEquals(Map.of("AX Conflict Zone", 1), counts);
    }

    @Test
    void countsCarriersTogetherInsteadOfNamingThem() {
        Map<String, Integer> counts = SystemSignalsFactSource.counted(List.of(
                location(signal("K7Q-BQL", null, "FleetCarrier"),
                        signal("V9T-40W", null, "SquadronCarrier"))));

        assertEquals(Map.of("fleet carrier", 2), counts);
    }

    /**
     * A {@code $...;} name is an identifier; with no localised name there is nothing safe to say.
     */
    @Test
    void dropsAGameCodeThatHasNoLocalisedName() {
        assertTrue(SystemSignalsFactSource.counted(List.of(
                location(signal("$MULTIPLAYER_SCENARIO77_TITLE;", null, "ResourceExtraction")))).isEmpty());
    }

    @Test
    void toleratesMissingRecords() {
        assertTrue(SystemSignalsFactSource.counted(null).isEmpty());
        assertTrue(SystemSignalsFactSource.counted(List.of()).isEmpty());
    }

    @Test
    void namesTheBusiestSignalFirstAndCountsOnlyWhatRepeats() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Nav Beacon", 1);
        counts.put("Resource Extraction Site [High]", 3);

        assertEquals("signals detected in this system: 3 x Resource Extraction Site [High], Nav Beacon",
                SystemSignalsFactSource.format(counts));
    }

    /**
     * The carrier entry is the one label this class writes itself, so it is the one that has to read correctly in
     * both numbers. A game-supplied name keeps the "N x" form, because pluralizing a proper name would invent one.
     */
    @Test
    void theCarrierCountReadsCorrectlyInBothNumbers() {
        assertEquals("signals detected in this system: fleet carrier",
                SystemSignalsFactSource.format(Map.of("fleet carrier", 1)));
        assertEquals("signals detected in this system: 3 fleet carriers",
                SystemSignalsFactSource.format(Map.of("fleet carrier", 3)));
        assertEquals("signals detected in this system: 2 x Resource Extraction Site [High]",
                SystemSignalsFactSource.format(Map.of("Resource Extraction Site [High]", 2)));
    }

    @Test
    void emptyWhenNothingHasBeenDetected() {
        assertTrue(SystemSignalsFactSource.format(Map.of()).isEmpty());
    }

    /**
     * It answers a signals question; where the commander is has its own sources.
     */
    @Test
    void isASubjectSourceRatherThanStandingContext() {
        assertFalse(new SystemSignalsFactSource().isAmbient());
    }
}
