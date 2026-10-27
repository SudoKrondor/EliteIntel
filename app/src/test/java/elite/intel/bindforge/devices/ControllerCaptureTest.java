package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.ControllerCapture.Captured;
import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.GameAxis;
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
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of())), button(STICK, 5, true));
    }

    @Test
    void aReleaseCapturesNothing() {
        button(STICK, 5, true);
        assertEquals(Optional.empty(), button(STICK, 5, false));
    }

    @Test
    void buttonsHeldOnAnyControllerBecomeModifiersInPressOrder() {
        button(THROTTLE, 0, true);
        button(STICK, 1, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of(input(THROTTLE, "Joy_1"), input(STICK, "Joy_2")))),
                button(STICK, 5, true));
    }

    @Test
    void aReleasedButtonIsNoLongerAModifier() {
        button(STICK, 0, true);
        button(STICK, 0, false);

        assertEquals(List.of(), button(STICK, 5, true).orElseThrow().modifiers());
    }

    @Test
    void atMostThreeModifiersAreKept() {
        for (int b = 0; b < 4; b++) button(STICK, b, true);

        assertEquals(3, button(STICK, 9, true).orElseThrow().modifiers().size());
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
                button(STICK, 0, true).orElseThrow().modifiers());
    }

    @Test
    void anUnpluggedControllerHoldsNothing() {
        button(STICK, 0, true);
        capture.release(STICK);

        assertEquals(List.of(), button(THROTTLE, 0, true).orElseThrow().modifiers());
    }

    @Test
    void anAxisMovementCapturesItsDirection() {
        assertEquals(Optional.empty(), capture.axis(STICK, GameAxis.RX, 0.0f), "where it rests");
        assertEquals(Optional.of(new Captured(input(STICK, "Pos_Joy_RXAxis"), List.of())),
                capture.axis(STICK, GameAxis.RX, 0.8f));
        assertEquals(Optional.empty(), capture.axis(STICK, GameAxis.RX, 0.9f));
        assertEquals(Optional.empty(), capture.axis(STICK, GameAxis.RX, 0.0f));
        assertEquals(Optional.of(new Captured(input(STICK, "Neg_Joy_RXAxis"), List.of())),
                capture.axis(STICK, GameAxis.RX, -0.8f));
    }

    /** Krondor's review, 2026-10-27: measured from where the axis rests, so a parked throttle is not captured. */
    @Test
    void anAxisRestingAtOneEndIsNotCapturedUntilItIsMovedFromThere() {
        assertEquals(Optional.empty(), capture.axis(THROTTLE, GameAxis.Z, -1.0f), "parked at one end");
        assertEquals(Optional.empty(), capture.axis(THROTTLE, GameAxis.Z, -0.95f), "jitter");
        assertEquals(Optional.of(new Captured(input(THROTTLE, "Pos_Joy_ZAxis"), List.of())),
                capture.axis(THROTTLE, GameAxis.Z, -0.4f), "pushed forward, away from where it rests");
    }

    @Test
    void anUnpluggedControllerForgetsWhereItsAxesRest() {
        capture.axis(THROTTLE, GameAxis.Z, -1.0f);
        capture.release(THROTTLE);

        assertEquals(Optional.empty(), capture.axis(THROTTLE, GameAxis.Z, 0.8f), "a new rest, not a push");
    }

    @Test
    void changingAxisDirectionDoesNotMakeTheOldDirectionAModifier() {
        capture.axis(STICK, GameAxis.RZ, 0.0f);
        capture.axis(STICK, GameAxis.RZ, -0.8f);
        assertEquals(List.of(), capture.axis(STICK, GameAxis.RZ, 0.8f).orElseThrow().modifiers());
    }

    @Test
    void aGamePadTriggerIsCapturedUnderItsOwnTokenOnceItIsPulled() {
        assertEquals(Optional.of(new Captured(input("GamePad", "Pos_GamePad_RTrigger"), List.of())),
                capture.gamepadAxis("GamePad", GameAxis.RZ, 0.8f));
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.RZ, 0.9f));
        assertEquals(Optional.of(new Captured(input("GamePad", "Pos_GamePad_LTrigger"), List.of())),
                capture.gamepadAxis("GamePad", GameAxis.Z, 0.8f));
    }

    @Test
    void aGamePadTriggerAtRestIsNotAPress() {
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.Z, -1.0f));
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.Z, -0.5f));
        assertEquals(Optional.of(new Captured(input("GamePad", "Pos_GamePad_LTrigger"), List.of())),
                capture.gamepadAxis("GamePad", GameAxis.Z, 0.5f));
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.Z, -1.0f));
        assertEquals(Optional.of(new Captured(input("GamePad", "Pos_GamePad_LTrigger"), List.of())),
                capture.gamepadAxis("GamePad", GameAxis.Z, 0.5f), "pulled again after it went back to rest");
    }

    /** Alan, 2026-10-27: a pad never uses Joy_ tokens, and its own stick tokens are not measured yet. */
    @Test
    void aGamePadStickIsNotCaptured() {
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.X, 0.0f));
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.X, -0.8f));
        assertEquals(Optional.empty(), capture.gamepadAxis("GamePad", GameAxis.RY, 0.9f));
    }

    @Test
    void gamePadTriggerReleaseDoesNotBecomeAJoystickModifier() {
        capture.gamepadAxis("GamePad", GameAxis.Z, 0.8f);
        capture.gamepadAxis("GamePad", GameAxis.Z, -1.0f);
        assertEquals(List.of(), capture.gamepadAxis("GamePad", GameAxis.RZ, 0.8f).orElseThrow().modifiers());
    }

    @Test
    void aGamePadHeldTriggerIsNotAModifier() {
        capture.gamepadAxis("GamePad", GameAxis.Z, 0.8f);
        assertEquals(List.of(), button("GamePad", 0, true).orElseThrow().modifiers());
    }

    @Test
    void gamePadFaceAndShoulderButtonsUseGamePadTokens() {
        assertEquals("GamePad_FaceDown", gamepadButton(0));
        assertEquals("GamePad_FaceRight", gamepadButton(1));
        assertEquals("GamePad_FaceLeft", gamepadButton(2));
        assertEquals("GamePad_FaceUp", gamepadButton(3));
        assertEquals("GamePad_LBumper", gamepadButton(4));
        assertEquals("GamePad_RBumper", gamepadButton(5));
        assertEquals("GamePad_Back", gamepadButton(6));
        assertEquals("GamePad_LThumb", gamepadButton(8));
        assertEquals("GamePad_RThumb", gamepadButton(9));
    }

    @Test
    void gamePadStartAndGuideCannotBeBound() {
        assertEquals(Optional.empty(), ControllerCapture.buttonToken(true, 7));
        assertEquals(Optional.empty(), ControllerCapture.buttonToken(true, 10));
    }

    @Test
    void buttonsPastTheGamePadLayoutBindAsPlainButtons() {
        assertEquals("Joy_12", gamepadButton(11));
    }

    @Test
    void anOrdinaryControllersButtonsAreNumberedFromOne() {
        assertEquals(Optional.of("Joy_8"), ControllerCapture.buttonToken(false, 7));
    }

    @Test
    void gamePadDPadUsesGamePadTokens() {
        assertEquals(Optional.of(new Captured(input("GamePad", "GamePad_DPadRight"), List.of())),
                new ControllerCapture().gamepadHat("GamePad", 0, DeviceHatEvent.RIGHT));
        assertEquals(List.of("GamePad_DPadUp", "GamePad_DPadRight", "GamePad_DPadDown", "GamePad_DPadLeft"),
                ControllerCapture.gamepadDPadTokens());
    }

    @Test
    void onlyTheZAndRzAxesAreGamePadTriggers() {
        assertEquals(Optional.of("Pos_GamePad_LTrigger"), ControllerCapture.gamepadTriggerToken(GameAxis.Z));
        assertEquals(Optional.of("Pos_GamePad_RTrigger"), ControllerCapture.gamepadTriggerToken(GameAxis.RZ));
        assertEquals(Optional.empty(), ControllerCapture.gamepadTriggerToken(GameAxis.X));
        assertEquals(Optional.of(GameAxis.Z), ControllerCapture.gamepadTriggerAxis("Pos_GamePad_LTrigger"));
        assertEquals(Optional.empty(), ControllerCapture.gamepadTriggerAxis("Pos_Joy_ZAxis"));
    }

    @Test
    void anAxisHeldFromAnEarlierMotionIsNeverAnAxisModifier() {
        capture.axis(STICK, GameAxis.RX, 0.0f);
        capture.axis(STICK, GameAxis.RX, -0.8f);
        capture.axis(STICK, GameAxis.RY, 0.0f);
        assertEquals(List.of(), capture.axis(STICK, GameAxis.RY, 0.8f).orElseThrow().modifiers());
    }

    private static BindingInput input(String device, String key) {
        return new BindingInput(device, key);
    }

    private Optional<Captured> button(String device, int sdlIndex, boolean pressed) {
        return capture.button(device, ButtonMapLabels.buttonToken(sdlIndex + 1), pressed);
    }

    private static String gamepadButton(int sdlIndex) {
        return ControllerCapture.buttonToken(true, sdlIndex).orElseThrow();
    }
}
