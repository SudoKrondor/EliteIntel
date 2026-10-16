package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.ControllerCapture.Captured;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.devices.events.DeviceHatEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Capture is a single motion, as in the game: whatever is held when the completing input arrives becomes its
 * modifiers.
 */
class ControllerCaptureTest {

    private static final String STICK = "VKB-Stick";
    private static final String THROTTLE = "VPCThrottle";
    private final ControllerCapture capture = new ControllerCapture();

    @Test
    void aButtonPressIsItsGameTokenNumberedFromOne() {
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of())), capture.button(STICK, 5, true));
    }

    @Test
    void aReleaseCapturesNothing() {
        capture.button(STICK, 5, true);
        assertEquals(Optional.empty(), capture.button(STICK, 5, false));
    }

    @Test
    void buttonsHeldOnAnyControllerBecomeModifiersInPressOrder() {
        capture.button(THROTTLE, 0, true);
        capture.button(STICK, 1, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of(input(THROTTLE, "Joy_1"), input(STICK, "Joy_2")))),
                capture.button(STICK, 5, true));
    }

    @Test
    void aReleasedButtonIsNoLongerAModifier() {
        capture.button(STICK, 0, true);
        capture.button(STICK, 0, false);

        assertEquals(List.of(), capture.button(STICK, 5, true).orElseThrow().modifiers());
    }

    @Test
    void atMostThreeModifiersAreKept() {
        for (int b = 0; b < 4; b++) capture.button(STICK, b, true);

        assertEquals(3, capture.button(STICK, 9, true).orElseThrow().modifiers().size());
    }

    @Test
    void aHatDirectionIsAPovToken() {
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV1Up"), List.of())),
                capture.hat(STICK, 0, DeviceHatEvent.UP));
        assertEquals(Optional.empty(), capture.hat(STICK, 0, 0));
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV2Left"), List.of())),
                capture.hat(STICK, 1, DeviceHatEvent.LEFT));
    }

    @Test
    void aDiagonalCapturesTheFirstDirectionAndHoldsTheOther() {
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV1Up"), List.of())),
                capture.hat(STICK, 0, DeviceHatEvent.UP | DeviceHatEvent.RIGHT));
        assertEquals(List.of(input(STICK, "Joy_POV1Up"), input(STICK, "Joy_POV1Right")),
                capture.button(STICK, 0, true).orElseThrow().modifiers());
    }

    @Test
    void anUnpluggedControllerHoldsNothing() {
        capture.button(STICK, 0, true);
        capture.release(STICK);

        assertEquals(List.of(), capture.button(THROTTLE, 0, true).orElseThrow().modifiers());
    }

    private static BindingInput input(String device, String key) {
        return new BindingInput(device, key);
    }
}
