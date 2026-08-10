package elite.intel.gameapi.search.spansh.station;

import elite.intel.gameapi.search.spansh.station.interstellarfactors.InterstellarFactorsResultDto;
import elite.intel.gameapi.search.spansh.station.traderandbroker.TraderAndBrokerSearchDto;
import elite.intel.gameapi.search.spansh.station.vista.VistaGenomicsLocationDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the stale-Spansh loop: a decommissioned service stays listed for weeks, is the nearest hit,
 * and so keeps routing the commander back to the port they just found empty. Nothing in the system
 * they are currently in may come back as the destination, on any of the three station searches.
 */
class CurrentSystemFilterTest {

    private static TraderAndBrokerSearchDto.Result trader(String station, String system, double distance) {
        return GsonFactory.getGson().fromJson(json(station, system, distance), TraderAndBrokerSearchDto.Result.class);
    }

    private static InterstellarFactorsResultDto.Result factors(String station, String system, double distance) {
        return GsonFactory.getGson().fromJson(json(station, system, distance), InterstellarFactorsResultDto.Result.class);
    }

    private static VistaGenomicsLocationDto.Result vista(String station, String system, double distance) {
        return GsonFactory.getGson().fromJson(json(station, system, distance), VistaGenomicsLocationDto.Result.class);
    }

    private static String json(String station, String system, double distance) {
        return """
                {"name":"%s","system_name":"%s","distance":%s}
                """.formatted(station, system, distance);
    }

    @Test
    @DisplayName("the station we are docked at is dropped, the next system out becomes the destination")
    void dropsTheStationWeAreStandingOn() {
        List<TraderAndBrokerSearchDto.Result> kept = CurrentSystemFilter.exclude(
                List.of(trader("Ray Gateway", "Diaguandri", 0.0),
                        trader("Ehrlich Terminal", "LHS 3447", 12.4)),
                "Diaguandri");

        assertEquals(1, kept.size());
        assertEquals("Ehrlich Terminal", kept.getFirst().getStationName());
    }

    @Test
    @DisplayName("another port in the same system goes too, not just the pad we are parked on")
    void dropsSiblingStationsInTheSameSystem() {
        List<TraderAndBrokerSearchDto.Result> kept = CurrentSystemFilter.exclude(
                List.of(trader("Ray Gateway", "Diaguandri", 0.0),
                        trader("Kirk Dock", "Diaguandri", 0.0),
                        trader("Ehrlich Terminal", "LHS 3447", 12.4)),
                "Diaguandri");

        assertEquals(1, kept.size());
        assertEquals("LHS 3447", kept.getFirst().getSystemName());
    }

    @Test
    @DisplayName("interstellar factors hits are filtered the same way")
    void filtersInterstellarFactors() {
        List<InterstellarFactorsResultDto.Result> kept = CurrentSystemFilter.exclude(
                List.of(factors("Ray Gateway", "Diaguandri", 0.0),
                        factors("Ehrlich Terminal", "LHS 3447", 12.4)),
                "Diaguandri");

        assertEquals(1, kept.size());
        assertEquals("Ehrlich Terminal", kept.getFirst().getStationName());
    }

    @Test
    @DisplayName("vista genomics hits are filtered the same way")
    void filtersVistaGenomics() {
        List<VistaGenomicsLocationDto.Result> kept = CurrentSystemFilter.exclude(
                List.of(vista("Ray Gateway", "Diaguandri", 0.0),
                        vista("Ehrlich Terminal", "LHS 3447", 12.4)),
                "Diaguandri");

        assertEquals(1, kept.size());
        assertEquals("Ehrlich Terminal", kept.getFirst().getStationName());
    }

    @Test
    @DisplayName("everything in range can be excluded, leaving nothing")
    void canExcludeEveryHit() {
        assertTrue(CurrentSystemFilter.exclude(
                List.of(trader("Ray Gateway", "Diaguandri", 0.0),
                        trader("Kirk Dock", "Diaguandri", 0.0)),
                "Diaguandri").isEmpty());
    }

    @Test
    @DisplayName("a null hit list is an empty result, not a crash")
    void nullHitsBecomeEmpty() {
        assertTrue(CurrentSystemFilter.<TraderAndBrokerSearchDto.Result>exclude(null, "Diaguandri").isEmpty());
    }

    @Test
    @DisplayName("zero distance is same-system on its own, without trusting the session")
    void zeroDistanceAloneExcludes() {
        // The distance comes back measured from the coordinates the search was given, so it stays
        // right even when the session's idea of which system we are in has not caught up.
        assertTrue(CurrentSystemFilter.isCurrentSystem(trader("Ray Gateway", "Diaguandri", 0.0), null));
        assertTrue(CurrentSystemFilter.isCurrentSystem(trader("Ray Gateway", "Diaguandri", 0.0), "Sol"));
    }

    @Test
    @DisplayName("the name match covers a hit whose distance was computed from stale coordinates")
    void nameMatchCatchesStaleCoordinates() {
        assertTrue(CurrentSystemFilter.isCurrentSystem(trader("Ray Gateway", " diaguandri ", 3.2), "Diaguandri"));
    }

    @Test
    @DisplayName("a real destination in another system survives both tests")
    void keepsGenuineDestinations() {
        assertFalse(CurrentSystemFilter.isCurrentSystem(trader("Ehrlich Terminal", "LHS 3447", 12.4), "Diaguandri"));
        assertFalse(CurrentSystemFilter.isCurrentSystem(trader("Ehrlich Terminal", "LHS 3447", 12.4), null));
    }

    @Test
    @DisplayName("an unknown current system still leaves the distance test doing its job")
    void unknownCurrentSystemDoesNotExcludeEverything() {
        List<TraderAndBrokerSearchDto.Result> kept = CurrentSystemFilter.exclude(
                List.of(trader("Ray Gateway", "Diaguandri", 0.0),
                        trader("Ehrlich Terminal", "LHS 3447", 12.4)),
                null);

        assertEquals(1, kept.size());
    }
}
