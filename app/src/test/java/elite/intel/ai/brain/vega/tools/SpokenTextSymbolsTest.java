package elite.intel.ai.brain.vega.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The characters here are the ones a commander hears rather than reads past, so every case is written as
 * the line that was spoken wrongly in the field.
 */
class SpokenTextSymbolsTest {

    @Test
    void emDashBesideAFigureBecomesAPauseRatherThanAMinusSign() {
        // Reported 2026-09-22: voiced as "extreme cold minus 273 C", which inverts the reading.
        assertEquals("Extreme cold, 273 C. Proceed with caution.",
                SpokenTextSymbols.strip("Extreme cold—273 C. Proceed with caution."));
    }

    @Test
    void emDashJoiningTwoClausesBecomesAComma() {
        assertEquals("Col 285 SW L b8 4 1, tidally locked, 0.05G surface gravity.",
                SpokenTextSymbols.strip("Col 285 SW L b8 4 1—tidally locked, 0.05G surface gravity."));
    }

    @Test
    void spacingAroundTheDashDoesNotChangeTheResult() {
        String expected = "Eleven materials detected, proceed with caution.";
        assertEquals(expected, SpokenTextSymbols.strip("Eleven materials detected — proceed with caution."));
        assertEquals(expected, SpokenTextSymbols.strip("Eleven materials detected— proceed with caution."));
        assertEquals(expected, SpokenTextSymbols.strip("Eleven materials detected –proceed with caution."));
    }

    @Test
    void emphasisMarkersAreDroppedRatherThanVoicedByName() {
        // "asterisk Jameson Memorial asterisk" is what the commander heard.
        assertEquals("Docking granted at Jameson Memorial.",
                SpokenTextSymbols.strip("Docking granted at *Jameson Memorial*."));
        assertEquals("Docking granted at Jameson Memorial.",
                SpokenTextSymbols.strip("Docking granted at **Jameson Memorial**."));
    }

    @Test
    void quotationMarksAreDroppedInEveryShapeAModelReachesFor() {
        assertEquals("Route set for Shinrarta Dezhra.",
                SpokenTextSymbols.strip("Route set for \"Shinrarta Dezhra\"."));
        assertEquals("Route set for Shinrarta Dezhra.",
                SpokenTextSymbols.strip("Route set for “Shinrarta Dezhra”."));
        // Guillemets, which the French and Russian commander languages use for quotation.
        assertEquals("Route set for Shinrarta Dezhra.",
                SpokenTextSymbols.strip("Route set for «Shinrarta Dezhra»."));
    }

    @Test
    void apostrophesHyphensAndTheMinusSignAreLeftAlone() {
        // All three appear inside names and readings the engines already pronounce correctly, and the
        // minus sign proper means minus - which is the right reading, unlike the em dash.
        String line = "Commander's request: Col 285 Sector AA-A h1 is at −273 C.";
        assertSame(line, SpokenTextSymbols.strip(line));
    }

    @Test
    void aLineWithNothingToStripIsReturnedUnchanged() {
        String line = "Docking request granted, pad 04.";
        assertSame(line, SpokenTextSymbols.strip(line));
    }

    @Test
    void aDashOpeningOrClosingTheLineLeavesNoStrandedComma() {
        assertEquals("tidally locked", SpokenTextSymbols.strip("—tidally locked"));
        assertEquals("tidally locked", SpokenTextSymbols.strip("tidally locked —"));
    }

    @Test
    void aDashNextToPunctuationDoesNotDoubleIt() {
        assertEquals("Scan complete. Eleven materials.",
                SpokenTextSymbols.strip("Scan complete —. Eleven materials."));
    }

    @Test
    void nullAndEmptyNeedNoGuardFromCallers() {
        assertEquals("", SpokenTextSymbols.strip(null));
        assertEquals("", SpokenTextSymbols.strip(""));
    }
}
