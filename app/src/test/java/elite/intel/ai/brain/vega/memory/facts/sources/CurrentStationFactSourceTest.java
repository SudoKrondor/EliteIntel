package elite.intel.ai.brain.vega.memory.facts.sources;

import elite.intel.ai.brain.vega.memory.facts.sources.CurrentStationFactSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentStationFactSourceTest {

    @Test
    void buildsACompactLineFromAllFields() {
        assertEquals(
                "current station Jameson Memorial: coriolis starport, economy high tech, faction pilots federation, 14 services",
                CurrentStationFactSource.format("Jameson Memorial", "coriolis starport", "high tech", "pilots federation", 14));
    }

    @Test
    void skipsEmptyAndUnknownFields() {
        assertEquals("current station Jameson Memorial",
                CurrentStationFactSource.format("Jameson Memorial", "  ", "unknown", null, 0));
    }

    @Test
    void keepsFactWithoutNameWhenAttributesExist() {
        assertEquals("current station: outpost, 3 services",
                CurrentStationFactSource.format(null, "outpost", null, null, 3));
    }

    @Test
    void usesTheSingularForOneService() {
        assertEquals("current station x: 1 service",
                CurrentStationFactSource.format("x", null, null, null, 1));
    }

    /**
     * The cap covers the head as well, so a head built from an unbounded journal string cannot carry the line past
     * the limit the facts block relies on.
     */
    @Test
    void anOversizedHeadIsShortenedRatherThanBreakingTheCap() {
        String line = CurrentStationFactSource.format("A very long station name ".repeat(20), "outpost", null, null, 0);

        assertTrue(line.length() <= FactLine.MAX_CHARS);
        assertTrue(line.endsWith("..."));
    }

    @Test
    void shortenedKeepsWholeWordsAndLeavesShortTextAlone() {
        assertEquals("station name", FactLine.shortened("station name", 20));
        assertEquals("the quick...", FactLine.shortened("the quick brown fox", 12));
        // A single word with no boundary to cut at still has to fit.
        assertEquals("abcdefg...", FactLine.shortened("abcdefghijklmnop", 10));
    }

    @Test
    void emptyWhenNoNameAndNoAttributes() {
        assertTrue(CurrentStationFactSource.format("", null, null, null, 0).isEmpty());
        assertTrue(CurrentStationFactSource.format(null, "  ", "unknown", null, 0).isEmpty());
    }
}
