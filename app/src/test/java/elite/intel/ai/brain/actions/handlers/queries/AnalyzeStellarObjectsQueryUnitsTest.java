package elite.intel.ai.brain.actions.handlers.queries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The LLM reads these two fields straight out of the payload, so a value that rounds away or a unit fault that survives
 * into the prompt is spoken to the commander as fact.
 */
class AnalyzeStellarObjectsQueryUnitsTest {

    @Test
    void keepsGravityBelowHalfAGravity() {
        // Rounding to a whole number reported every body under 0.5g as 0, which the prompt reads as "no data" --
        // and most landable bodies are under 0.5g.
        assertEquals(0.35, AnalyzeStellarObjectsQuery.toEarthGravities(0.35));
        assertEquals(0.36, AnalyzeStellarObjectsQuery.toEarthGravities(0.3554));
        assertEquals(0.03, AnalyzeStellarObjectsQuery.toEarthGravities(0.0349));
        assertEquals(6.33, AnalyzeStellarObjectsQuery.toEarthGravities(6.32578258));
    }

    @Test
    void omitsGravityThatIsUnknownOrImpossible() {
        assertNull(AnalyzeStellarObjectsQuery.toEarthGravities(0));
        assertNull(AnalyzeStellarObjectsQuery.toEarthGravities(355396.56));
    }

    @Test
    void convertsKelvinToCelsius() {
        assertEquals(-99L, AnalyzeStellarObjectsQuery.toCelsius(174.0));
        assertEquals(37L, AnalyzeStellarObjectsQuery.toCelsius(310.0));
    }

    @Test
    void omitsTemperatureThatIsUnknownOrBelowAbsoluteZero() {
        assertNull(AnalyzeStellarObjectsQuery.toCelsius(0));
        assertNull(AnalyzeStellarObjectsQuery.toCelsius(-1464.0), "the double-converted temperature of migration 01040");
    }
}
