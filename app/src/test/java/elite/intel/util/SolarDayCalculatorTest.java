package elite.intel.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolarDayCalculatorTest {

    private static final double DAY = 86400.0;

    @Test
    void tidalLockUsesSiderealRotation() {
        assertEquals(DAY, SolarDayCalculator.solarDaySeconds(DAY, 10 * DAY, true), 1e-6);
    }

    @Test
    void missingOrbitFallsBackToRotation() {
        assertEquals(DAY, SolarDayCalculator.solarDaySeconds(DAY, 0, false), 1e-6);
        assertEquals(DAY, SolarDayCalculator.solarDaySeconds(-DAY, -5, false), 1e-6);
    }

    @Test
    void tooFastRotationIsUnknown() {
        assertEquals(0, SolarDayCalculator.solarDaySeconds(30, 500 * DAY, false), 1e-6);
    }

    @Test
    void synchronousRotationEqualsSiderealDay() {
        assertEquals(DAY, SolarDayCalculator.solarDaySeconds(DAY, DAY, false), 1e-6);
    }

    @Test
    void progradeSolarDayIsSlightlyLongerThanSidereal() {
        // 1-day sidereal rotation, 365-day orbit: apparent day is a touch over a day.
        assertEquals(86637.4, SolarDayCalculator.solarDaySeconds(DAY, 365 * DAY, false), 1.0);
    }

    @Test
    void retrogradeSolarDayIsShorterThanSidereal() {
        assertEquals(86164.1, SolarDayCalculator.solarDaySeconds(-DAY, 365 * DAY, false), 1.0);
    }
}
