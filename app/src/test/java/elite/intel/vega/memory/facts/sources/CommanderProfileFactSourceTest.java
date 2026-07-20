package elite.intel.vega.memory.facts.sources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommanderProfileFactSourceTest {

    @Test
    void buildsAFullCommanderLineInPriorityOrder() {
        String result = CommanderProfileFactSource.format("CMDR", "Federal", "Elite", "Elite", "Elite", "Elite",
                "Aisling", 500_000L);
        assertEquals(
                "commander CMDR: allegiance Federal, combat rank Elite, exploration rank Elite, credits 500K cr, pledged to Aisling, exobiology rank Elite, mercenary rank Elite",
                result);
        assertTrue(result.length() <= FactLine.MAX_CHARS);
    }

    @Test
    void skipsEmptyAndUnknownFields() {
        assertEquals("commander Jameson: combat rank Elite",
                CommanderProfileFactSource.format("Jameson", "unknown", "Elite", "  ", null, "unknown", null, 0));
    }

    @Test
    void formatsCreditsCompactly() {
        assertEquals("commander Jameson: credits 1.2M cr",
                CommanderProfileFactSource.format("Jameson", null, null, null, null, null, null, 1_200_000L));
        assertEquals("commander Jameson: credits 340K cr",
                CommanderProfileFactSource.format("Jameson", null, null, null, null, null, null, 340_000L));
        assertEquals("commander Jameson: credits 500 cr",
                CommanderProfileFactSource.format("Jameson", null, null, null, null, null, null, 500L));
    }

    @Test
    void capsLengthStoppingAtTheFieldThatDoesNotFit() {
        String longPower = "P".repeat(200);
        String result = CommanderProfileFactSource.format("Jameson", "Federation", "Elite", "Pioneer", "Ecologist",
                "Rookie", longPower, 1_200_000_000L);

        assertEquals("commander Jameson: allegiance Federation, combat rank Elite, exploration rank Pioneer, credits 1.2B cr",
                result);
        assertTrue(result.length() <= FactLine.MAX_CHARS);
    }

    @Test
    void emptyWhenNameUnknown() {
        assertTrue(CommanderProfileFactSource.format("", "Federation", "Elite", null, null, null, null, 1L).isEmpty());
        assertTrue(CommanderProfileFactSource.format("unknown", "Federation", "Elite", null, null, null, null, 1L).isEmpty());
    }
}
