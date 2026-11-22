package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.ControllerCapture.Captured;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A spring-loaded axis pushed one way and let go must leave that direction picked (Krondor, 2026-11-10: pulling a stick's
 * RZ up picked positive, and letting the spring take it back to centre picked negative). Each test feeds the values
 * the Device Service would report, in order, and checks what the dialog would hold at the end - the last direction
 * captured.
 */
class ControllerCaptureSpringReturnTest {

    private static final String STICK = "VKB-Stick";

    /**
     * A still stick reports nothing, so the first value capture sees is already part-way through the push. With the
     * rest read from the device when the dialog opens, the return to centre is a return, not a push the other way.
     */
    @Test
    void aPushWhoseFirstReportIsMidTravelKeepsItsDirectionWhenTheRestIsKnown() {
        ControllerCapture capture = new ControllerCapture();
        capture.rest(STICK, GameAxis.RZ, 0.0f);

        assertEquals("Pos_Joy_RZAxis", lastCaptured(capture, 0.30f, 0.70f, 1.00f, 0.50f, 0.00f));
    }

    /** With no rest given - a controller plugged in while the dialog is open - the first value seen is still the rest. */
    @Test
    void withNoRestGivenTheFirstValueSeenIsTheRest() {
        ControllerCapture capture = new ControllerCapture();

        assertEquals(Optional.empty(), capture.axis(STICK, GameAxis.RZ, -1.0f));
        assertEquals("Pos_Joy_RZAxis", lastCaptured(capture, -0.4f));
    }

    private static String lastCaptured(ControllerCapture capture, float... values) {
        String last = null;
        for (float value : values) {
            Optional<Captured> captured = capture.axis(STICK, GameAxis.RZ, value);
            if (captured.isPresent()) last = captured.get().input().key();
        }
        return last;
    }
}
