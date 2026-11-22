package elite.intel.ai.brain.actions.handlers.queries;

import elite.intel.db.dao.ExoMasteryDao.Body;
import elite.intel.gameapi.journal.events.FSSBodySignalsEvent;
import elite.intel.gameapi.journal.events.dto.LocationDto;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
                List.of(), List.of(), List.of(), List.of(new AnalyzeBioScansStarSystemQuery.UnmappedPlanet("2 a", 3)));

        assertTrue(conclusion.startsWith("Nothing left to scan in this system"), conclusion);
        assertTrue(conclusion.contains("2 a (3 sampled) was never surface-mapped"), conclusion);
    }

    @Test
    @DisplayName("outstanding scans lead the conclusion")
    void outstandingScansLeadTheConclusion() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(
                List.of(), List.of(new AnalyzeBioScansStarSystemQuery.PlanetsToScan("B 4", 2)), List.of(), List.of());

        assertTrue(conclusion.endsWith("Still to scan: B 4 (2)."), conclusion);
    }

    /**
     * 76 Leonis: seven moons with seven signals each, one surveyed. "How many moons have bio signals" is the
     * question asked most, and a list of what is left cannot answer it - the total and the finished ones lead.
     */
    @Test
    @DisplayName("the conclusion states how many bodies carry biology and which are surveyed")
    void theConclusionStatesTheTotalAndTheSurveyedBodies() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(
                List.of("6 a"),
                List.of(new AnalyzeBioScansStarSystemQuery.PlanetsToScan("6 b", 7),
                        new AnalyzeBioScansStarSystemQuery.PlanetsToScan("6 c", 7)),
                List.of(), List.of());

        assertEquals("3 bodies carry bio signals; fully surveyed: 6 a. Still to scan: 6 b (7), 6 c (7).", conclusion);
    }

    @Test
    @DisplayName("a fully surveyed system says so with no caveat")
    void aFullySurveyedSystemHasNoCaveat() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(List.of(), List.of(), List.of(), List.of());

        assertEquals("Nothing left to scan in this system: every detected bio signal has been sampled.", conclusion);
    }

    /**
     * Pegasi Sector HC-U b3-8 (bundle 2026-11-12): routed in by Exo-Mastery, no FSS pass, A 6 sampled out. The query
     * answered "only A 6 carries bio signals, all done", and the commander wrote off five unsampled bodies on the
     * strength of it.
     */
    @Test
    @DisplayName("bodies the catalogue lists open, never scanned here, are outstanding work")
    void catalogueBodiesWithoutAScanOfOurOwnAreOutstanding() {
        String conclusion = AnalyzeBioScansStarSystemQuery.conclusion(
                List.of("A 6"), List.of(), List.of("A 7", "A 9", "B 1", "B 3", "B 4"), List.of());

        assertEquals("6 bodies carry bio signals; fully surveyed: A 6. The Exo-Mastery catalogue lists organics "
                + "still to sample on A 7, A 9, B 1, B 3, B 4, not scanned here yet.", conclusion);
    }

    @Test
    @DisplayName("our own scan of a body outranks the catalogue's entry for it")
    void ourScanOutranksTheCatalogue() {
        List<Body> open = List.of(body(6, "A 6"), body(7, "A 7"), body(9, "A 9"));

        List<Body> listed = AnalyzeBioScansStarSystemQuery.cataloguedWithoutVerdict(open, Set.of(6L, 9L));

        assertEquals(List.of(7L), listed.stream().map(Body::bodyId).toList());
    }

    private static Body body(long bodyId, String shortName) {
        return new Body(1234L, bodyId, "Pegasi Sector HC-U b3-8", "Pegasi Sector HC-U b3-8 " + shortName,
                "Icy body", 20_000_000L, false);
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