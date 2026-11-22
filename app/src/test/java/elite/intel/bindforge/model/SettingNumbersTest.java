package elite.intel.bindforge.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static elite.intel.bindforge.model.SettingNumbers.Refusal.*;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void showsTheNumberAFloatWasRoundedFrom() {
        assertEquals("0.4", SettingNumbers.shown("0.40000001"));
        assertEquals("0.9", SettingNumbers.shown("0.89999998"));
        assertEquals("0.00000001", SettingNumbers.shown("0.00000001"));
        assertEquals("200.0", SettingNumbers.shown("200.00000000"));
    }

    @Test
    void everySettingHasAMeasuredRangeExceptMotionHeadlook() {
        Set<String> withoutRange = SettingNumbers.SETTINGS.stream()
                .filter(id -> SettingNumbers.range(id).isEmpty())
                .collect(Collectors.toSet());
        assertEquals(Set.of("HeadlookMotionSensitivity"), withoutRange);
    }

    @Test
    void rangesAreTheStopsMeasuredInTheGame() {
        assertRange("MouseSensitivity", 0.1f, 10f);
        assertRange("FreeCamMouseSensitivity", 0.1f, 20f);
        assertRange("PlacementCamMouseSensitivity", 0.1f, 20f);
        assertRange("FSSMouseSensitivity", 0.1f, 40f);
        assertRange("SAAThirdPersonMouseSensitivity", 0.1f, 200f);
        assertRange("HeadlookSensitivity", 0.25f, 4f);
        assertRange("YawToRollSensitivity", 0.1f, 0.9f);
        assertRange("FSSMouseDeadzone", 0f, 0.2f);
        assertRange("FSSMouseLinearity", 1f, 3f);
        assertRange("MouseDecayRate", 1f, 10f);
    }

    /**
     * WHY: HudSlider snaps to min + k * step, so a step that does not divide the range would leave the top stop out of reach.
     */
    @Test
    void everyStepLandsExactlyOnTheTopStop() {
        SettingNumbers.SETTINGS.forEach(id -> SettingNumbers.range(id).ifPresent(range ->
                assertEquals(0, (range.sliderMax() - range.sliderMin()) % range.sliderStep(), id)));
    }

    @Test
    void aSliderPositionWritesTheNumberTheGameWrites() {
        SettingNumbers.Range range = SettingNumbers.range("YawToRollSensitivity").orElseThrow();
        assertEquals(900, range.sliderMax());
        assertEquals("0.89999998", SettingNumbers.written(range.fromSlider(900)));
        assertEquals("0.10000000", SettingNumbers.written(range.fromSlider(100)));
    }

    @Test
    void aRangeRefusesNothingTheGameAccepts() {
        assertEquals(500f, SettingNumbers.judge("SAAThirdPersonMouseSensitivity", "500").value());
        assertFalse(SettingNumbers.range("SAAThirdPersonMouseSensitivity").orElseThrow().contains(500f));
        assertTrue(SettingNumbers.range("YawToRollSensitivity").orElseThrow().contains(Float.parseFloat("0.89999998")));
    }

    private static void assertRange(String setting, float min, float max) {
        SettingNumbers.Range range = SettingNumbers.range(setting).orElseThrow();
        assertEquals(min, range.min(), setting);
        assertEquals(max, range.max(), setting);
    }
}
