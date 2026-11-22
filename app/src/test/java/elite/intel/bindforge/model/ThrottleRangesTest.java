package elite.intel.bindforge.model;

import org.junit.jupiter.api.Test;

import static elite.intel.bindforge.model.ThrottleRanges.Reading.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrottleRangesTest {

    private static ThrottleRanges.Range range(String axis) {
        return ThrottleRanges.forAxis(axis).orElseThrow();
    }

    @Test
    void onlyTheThrottleAxesHaveARange() {
        assertEquals("ThrottleRange", range("ThrottleAxis").setting());
        assertEquals("BuggyThrottleRange", range("DriveSpeedAxis").setting());
        assertEquals("ThrottleRangeFreeCam", range("MoveFreeCamY").setting());
        assertTrue(ThrottleRanges.forAxis("YawAxisRaw").isEmpty());
    }

    @Test
    void emptyIsFullRange() {
        assertEquals(FULL_RANGE, range("ThrottleAxis").read(""));
        assertEquals(FULL_RANGE, range("ThrottleAxis").read("Bindings_ThrottleFullRange"));
        assertEquals("", range("ThrottleAxis").valueFor(FULL_RANGE));
    }

    @Test
    void eachFieldWritesItsOwnForwardOnlyToken() {
        assertEquals("Bindings_ThrottleForewardOnly", range("ThrottleAxis").valueFor(FORWARD_ONLY));
        assertEquals("Bindings_BuggyThrottleForewardOnly", range("DriveSpeedAxis").valueFor(FORWARD_ONLY));
        assertEquals("Bindings_ThrottleForewardOnlyFreeCam", range("MoveFreeCamY").valueFor(FORWARD_ONLY));
    }

    @Test
    void aHandEditIsUnrecognisedNotCorrected() {
        assertEquals(UNRECOGNISED, range("ThrottleAxis").read("Forward"));
        assertEquals(UNRECOGNISED, range("DriveSpeedAxis").read("Bindings_ThrottleForewardOnly"));
    }
}
