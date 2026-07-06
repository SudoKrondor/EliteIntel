package elite.intel.companion.memory.facts.sources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentSystemFactSourceTest {

    @Test
    void buildsAFullLineWhileTravellingTheSystem() {
        assertEquals(
                "current system Sol: allegiance Federation, security High, economy Refinery, population 22.7B, controlled by Zachary Hudson",
                CurrentSystemFactSource.format(false, "Sol", "Federation", "High Security", "Refinery", 22_700_000_000L, "Zachary Hudson"));
    }

    @Test
    void shrinksToGroundingLineWhenFocusedOnABodyOrStation() {
        assertEquals(
                "current system Sol: allegiance Federation, security High",
                CurrentSystemFactSource.format(true, "Sol", "Federation", "High Security", "Refinery", 22_700_000_000L, "Zachary Hudson"));
    }

    @Test
    void skipsEmptyAndUnknownFields() {
        assertEquals("current system Sol",
                CurrentSystemFactSource.format(false, "Sol", null, "unknown", "  ", 0, null));
    }

    @Test
    void cleansTheRawSecurityToken() {
        assertEquals("current system Sol: security high",
                CurrentSystemFactSource.format(false, "Sol", null, "$SYSTEM_SECURITY_high;", null, 0, null));
    }

    @Test
    void formatsPopulationCompactly() {
        assertEquals("current system Sol: population 1.2M",
                CurrentSystemFactSource.format(false, "Sol", null, null, null, 1_200_000L, null));
        assertEquals("current system Sol: population 340K",
                CurrentSystemFactSource.format(false, "Sol", null, null, null, 340_000L, null));
    }

    @Test
    void capsLengthDroppingFieldsThatDoNotFit() {
        String longPower = "P".repeat(200);
        String result = CurrentSystemFactSource.format(false, "Sol", "Federation", "High", "Refinery", 0, longPower);

        assertEquals("current system Sol: allegiance Federation, security High, economy Refinery", result);
        assertTrue(result.length() <= FactLine.MAX_CHARS);
    }

    @Test
    void emptyWhenSystemUnknown() {
        assertTrue(CurrentSystemFactSource.format(false, "", "Federation", "High", "Refinery", 1L, "Power").isEmpty());
        assertTrue(CurrentSystemFactSource.format(false, "unknown", "Federation", "High", "Refinery", 1L, "Power").isEmpty());
    }
}
