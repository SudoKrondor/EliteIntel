package elite.intel.ai.brain.actions.handlers.query;

import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bio-scan remainder arithmetic. The live failure this guards against: a moon whose organics were all sampled on
 * foot, with no FSS signal count ever recorded, reported "three remaining organics to scan" - the handler had computed
 * {@code 0 signals - 3 completed = -3} and the model spoke the magnitude.
 */
class AnalyzeBioScansStarSystemQueryTest {

    @Test
    @DisplayName("a fully sampled body with no recorded signal count has nothing remaining, never a negative")
    void aFullySampledBodyWithoutSignalDataHasNothingRemaining() {
        LocationDto moon = new LocationDto(15L, 1234L);
        moon.setBioSignals(0); // never recorded: the body was sampled on foot without an FSS pass

        assertEquals(0, AnalyzeBioScansStarSystemQuery.bioSignalsDetected(moon));
        assertEquals(0, AnalyzeBioScansStarSystemQuery.remainingOrganics(0, 3));
    }

    @Test
    @DisplayName("remaining is signals minus completed while scans are outstanding")
    void remainingCountsDownAsSamplesAreTaken() {
        assertEquals(3, AnalyzeBioScansStarSystemQuery.remainingOrganics(3, 0));
        assertEquals(1, AnalyzeBioScansStarSystemQuery.remainingOrganics(3, 2));
        assertEquals(0, AnalyzeBioScansStarSystemQuery.remainingOrganics(3, 3));
    }

    @Test
    @DisplayName("FSS bio signals are summed and non-bio signals ignored")
    void fssBioSignalsAreSummed() {
        LocationDto planet = new LocationDto(15L, 1234L);
        planet.setFssSignals(List.of(signal("Biological", 3), signal("Geological", 5)));

        assertEquals(3, AnalyzeBioScansStarSystemQuery.bioSignalsDetected(planet));
    }

    @Test
    @DisplayName("the DSS bio signal count is used when the FSS list is absent")
    void dssSignalCountIsUsedWhenFssListIsAbsent() {
        LocationDto planet = new LocationDto(15L, 1234L);
        planet.setBioSignals(2);

        assertEquals(2, AnalyzeBioScansStarSystemQuery.bioSignalsDetected(planet));
        assertEquals(2, AnalyzeBioScansStarSystemQuery.remainingOrganics(2, 0));
    }

    @Test
    @DisplayName("an unmapped, fully sampled body reads as done-with-a-caveat, never as a pending scan")
    void anUnmappedSampledBodyIsNotOutstandingWork() {
        // The live case: 2 a was sampled 3 times but never surface-mapped, so no signal count was ever emitted.
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(
                List.of(), List.of(new AnalyzeBioScansStarSystemQuery.UnmappedPlanet("2 a", 3)));

        assertTrue(conclusion.startsWith("Nothing left to scan in this system"), conclusion);
        assertTrue(conclusion.contains("2 a (3 sampled) was never surface-mapped"), conclusion);
    }

    @Test
    @DisplayName("outstanding scans lead the conclusion")
    void outstandingScansLeadTheConclusion() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(
                List.of(new AnalyzeBioScansStarSystemQuery.PlanetsToScan("B 4", 2)), List.of());

        assertTrue(conclusion.startsWith("Still to scan: B 4 (2)."), conclusion);
    }

    @Test
    @DisplayName("a fully surveyed system says so with no caveat")
    void aFullySurveyedSystemHasNoCaveat() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(List.of(), List.of());

        assertEquals("Nothing left to scan in this system: every detected bio signal has been sampled.", conclusion);
    }

    /**
     * Signals are journal-shaped and have no setters, so build them the way the parser does.
     */
    private static FSSBodySignalsEvent.Signal signal(String typeLocalised, int count) {
        String json = "{\"Type\":\"$SAA_SignalType_" + typeLocalised + ";\",\"Type_Localised\":\"" + typeLocalised
                + "\",\"Count\":" + count + "}";
        return GsonFactory.getGson().fromJson(json, FSSBodySignalsEvent.Signal.class);
    }
}