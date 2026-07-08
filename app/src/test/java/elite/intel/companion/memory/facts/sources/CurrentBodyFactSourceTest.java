package elite.intel.companion.memory.facts.sources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentBodyFactSourceTest {

    @Test
    void buildsACompactLineFromAllFields() {
        assertEquals(
                "current body 1 a: Icy body, landable, gravity 2.5g, atmosphere Nitrogen, -53°C, day 18h, 2 bio signals, 1 geo signal, terraformable, rings, tidally locked",
                CurrentBodyFactSource.format("1 a", "Icy body", true, 2.5, "Nitrogen", 220.0, 2, 1, true, true, true, 64800.0));
    }

    @Test
    void skipsEmptyAndDefaultFields() {
        assertEquals("current body 1 a",
                CurrentBodyFactSource.format("1 a", null, false, 0, "  ", 0, 0, 0, false, false, false, 0));
    }

    @Test
    void formatsGravityInEarthGravitiesTrimmingTrailingZeros() {
        assertEquals("current body 1 a: gravity 0.28g",
                CurrentBodyFactSource.format("1 a", null, false, 0.28, null, 0, 0, 0, false, false, false, 0));
        assertEquals("current body 1 a: gravity 1g",
                CurrentBodyFactSource.format("1 a", null, false, 1.0, null, 0, 0, 0, false, false, false, 0));
    }

    @Test
    void convertsTemperatureToCelsius() {
        assertEquals("current body x: 227°C",
                CurrentBodyFactSource.format("x", null, false, 0, null, 500.0, 0, 0, false, false, false, 0));
    }

    @Test
    void rendersSolarDayCompactly() {
        assertEquals("current body x: day 18h",
                CurrentBodyFactSource.format("x", null, false, 0, null, 0, 0, 0, false, false, false, 64800.0));
        assertEquals("current body x: day 6h 40m",
                CurrentBodyFactSource.format("x", null, false, 0, null, 0, 0, 0, false, false, false, 24000.0));
        assertEquals("current body x: day 30m",
                CurrentBodyFactSource.format("x", null, false, 0, null, 0, 0, 0, false, false, false, 1800.0));
    }

    @Test
    void addsTidallyLockedFlag() {
        assertEquals("current body x: tidally locked",
                CurrentBodyFactSource.format("x", null, false, 0, null, 0, 0, 0, false, false, true, 0));
    }

    @Test
    void keepsFactWithoutNameWhenAttributesExist() {
        assertEquals("current body: Icy body, landable, 3 bio signals",
                CurrentBodyFactSource.format(null, "Icy body", true, 0, null, 0, 3, 0, false, false, false, 0));
    }

    @Test
    void emptyWhenNoNameAndNoAttributes() {
        assertTrue(CurrentBodyFactSource.format("", null, false, 0, null, 0, 0, 0, false, false, false, 0).isEmpty());
        assertTrue(CurrentBodyFactSource.format(null, "  ", false, 0, "unknown", 0, 0, 0, false, false, false, 0).isEmpty());
    }

    @Test
    void capsLengthDroppingFieldsThatDoNotFit() {
        String longAtmosphere = "A".repeat(200);
        String result = CurrentBodyFactSource.format("1 a", "Icy body", true, 0, longAtmosphere, 0, 0, 0, false, false, false, 0);

        assertEquals("current body 1 a: Icy body, landable", result);
        assertTrue(result.length() <= FactLine.MAX_CHARS);
    }
}
