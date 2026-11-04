package elite.intel.bindforge.model;

import org.junit.jupiter.api.Test;

import static elite.intel.bindforge.model.SettingNumbers.Refusal.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingNumbersTest {

    @Test
    void aSensitivityMustBeAboveZeroWithNoUpperLimit() {
        assertEquals(30f, SettingNumbers.judge("SAAThirdPersonMouseSensitivity", "30").value());
        assertEquals(MUST_BE_ABOVE_ZERO, SettingNumbers.judge("MouseSensitivity", "0").refusal());
    }

    @Test
    void aDeadzoneIsZeroToOne() {
        assertEquals(0f, SettingNumbers.judge("MouseDeadzone", "0").value());
        assertEquals(MUST_BE_ZERO_TO_ONE, SettingNumbers.judge("BuggyTurretMouseDeadzone", "1.5").refusal());
    }

    @Test
    void takesEitherDecimalMarkAndRefusesWhatIsNotANumber() {
        assertEquals(1.5f, SettingNumbers.judge("MouseSensitivity", " 1,5 ").value());
        assertEquals(NOT_A_NUMBER, SettingNumbers.judge("MouseSensitivity", "fast").refusal());
        assertEquals(NOT_A_NUMBER, SettingNumbers.judge("MouseSensitivity", "NaN").refusal());
        assertEquals(NOT_A_NUMBER, SettingNumbers.judge("MouseSensitivity", "").refusal());
    }

    @Test
    void writesAsTheGameDoesAndShowsWithoutItsZeros() {
        assertEquals("0.30000001", SettingNumbers.written(0.3f));
        assertEquals("1.0", SettingNumbers.shown("1.00000000"));
        assertEquals("0.05725", SettingNumbers.shown("0.05725000"));
        assertEquals("30.0", SettingNumbers.shown("30.00000000"));
        assertEquals("oops", SettingNumbers.shown("oops"));
    }
}
