package elite.intel.ai.brain.actions.handlers.queries;

import elite.intel.ai.brain.actions.handlers.queries.AnalyzeStellarObjectsQuery.LocationData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The deterministic body resolver is what keeps a small local model from answering "is B 1 landable"
 * with a whole-system aggregate: when exactly one body is named we hand the model only that body.
 * These cases pin the STT/NATO normalisation and the longest-match / ambiguity rules.
 */
class AnalyzeStellarObjectsQueryResolveBodyTest {

    private static LocationData body(String shortName) {
        return new LocationData(
                shortName,
                AnalyzeStellarObjectsQuery.toPhonetic(shortName),
                "PLANET", "High metal content body", null, "Test System",
                false, false, 0, 0, "None", null, 0, 0, false, false, false);
    }

    // A representative multi-star system: A/B/C/D planets, plus a moon that must win over its planet.
    private static final List<LocationData> BODIES = List.of(
            body("A 1"), body("B 1"), body("B 1 a"), body("B 4"), body("C 1"), body("D 6"));

    private static String resolvedName(String utterance) {
        LocationData r = AnalyzeStellarObjectsQuery.resolveNamedBody(utterance, BODIES);
        return r == null ? null : r.stellarObjectName();
    }

    @Test
    void resolvesGluedShortForm() {
        assertEquals("B 1", resolvedName("is planet b1 landable"));
    }

    @Test
    void resolvesSpokenLetterAndNumberWord() {
        assertEquals("B 1", resolvedName("is planet b one landable"));
    }

    @Test
    void resolvesNatoWord() {
        assertEquals("B 1", resolvedName("is bravo one landable"));
    }

    @Test
    void tolueratesGarbledLandableSuffix() {
        // STT dropped the suffix ("landable" -> "land up") but the body token is intact.
        assertEquals("B 1", resolvedName("is planet b one land up"));
    }

    @Test
    void longestMatchPrefersMoonOverItsPlanet() {
        // "B 1 a" must beat "B 1" so a moon question doesn't collapse to the planet.
        assertEquals("B 1 a", resolvedName("is b one a landable"));
    }

    @Test
    void mapsSpokenFourToDigit() {
        assertEquals("B 4", resolvedName("is planet b for landable"));
    }

    @Test
    void bareStarLetterDoesNotFalseMatch() {
        // No digit in the utterance -> no numbered body should resolve.
        assertNull(resolvedName("is planet b landable"));
    }

    @Test
    void wholeSystemCountQuestionResolvesNothing() {
        assertNull(resolvedName("how many landable planets are there"));
    }

    @Test
    void twoNamedBodiesAreAmbiguous() {
        assertNull(resolvedName("compare b one and c one"));
    }

    @Test
    void unrelatedBodyIsNotMatched() {
        // Utterance names D 6; A 1 / B 1 must not sneak in.
        assertEquals("D 6", resolvedName("tell me about d six"));
    }

    // Single-star systems name bodies with a bare number ("1", "2") - no star letter.
    private static final List<LocationData> SINGLE_STAR = List.of(body("1"), body("2"), body("2 a"));

    @Test
    void resolvesBareNumberBodyInSingleStarSystem() {
        LocationData r = AnalyzeStellarObjectsQuery.resolveNamedBody("is planet 2 landable", SINGLE_STAR);
        assertEquals("2", r == null ? null : r.stellarObjectName());
    }

    @Test
    void moonBeatsBareNumberPlanetInSingleStarSystem() {
        LocationData r = AnalyzeStellarObjectsQuery.resolveNamedBody("is 2 a landable", SINGLE_STAR);
        assertEquals("2 a", r == null ? null : r.stellarObjectName());
    }
}