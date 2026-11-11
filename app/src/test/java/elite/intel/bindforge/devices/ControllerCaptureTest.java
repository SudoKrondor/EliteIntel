package elite.intel.bindforge.devices;

import elite.intel.bindforge.devicefiles.ButtonMapLabels;
import elite.intel.bindforge.devices.ControllerCapture.Captured;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.devices.events.DeviceHatEvent;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Capture is a single motion, as in the game: whatever is held when the completing input arrives becomes its
 * modifiers. A button or hat is picked when it is let go (E8); an axis direction the moment it moves.
 */
class ControllerCaptureTest {

    private static final String STICK = "VKB-Stick";
    private static final String THROTTLE = "VPCThrottle";
    private final ControllerCapture capture = new ControllerCapture();

    @Test
    void aButtonIsPickedWhenLetGoAsItsGameTokenNumberedFromOne() {
        assertEquals(Optional.empty(), button(STICK, 5, true), "a press picks nothing yet");
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of())), button(STICK, 5, false));
    }

    /** E8 (Alan, 2026-11-10): a chord is never picked first as the bare button held for it. */
    @Test
    void aChordIsPickedWholeAndNeverAsTheButtonHeldForIt() {
        assertEquals(Optional.empty(), button(STICK, 0, true));
        assertEquals(Optional.empty(), button(STICK, 5, true));

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of(input(STICK, "Joy_1")))),
                button(STICK, 5, false));
        assertEquals(Optional.empty(), button(STICK, 0, false), "nothing pressed since the pick");
    }

    @Test
    void lettingGoOfTheModifierFirstPicksTheSameChord() {
        button(STICK, 0, true);
        button(STICK, 5, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of(input(STICK, "Joy_1")))),
                button(STICK, 0, false));
        assertEquals(Optional.empty(), button(STICK, 5, false));
    }

    /** With the shift button still held, a second button pressed and let go is a second pick. */
    @Test
    void aShiftButtonHeldPicksEachButtonPressedWithIt() {
        button(STICK, 0, true);
        button(STICK, 5, true);
        button(STICK, 5, false);
        button(STICK, 6, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_7"), List.of(input(STICK, "Joy_1")))),
                button(STICK, 6, false));
    }

    @Test
    void buttonsHeldOnAnyControllerBecomeModifiersInPressOrder() {
        button(THROTTLE, 0, true);
        button(STICK, 1, true);
        button(STICK, 5, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_6"), List.of(input(THROTTLE, "Joy_1"), input(STICK, "Joy_2")))),
                button(STICK, 5, false));
    }

    @Test
    void aReleasedButtonIsNoLongerAModifier() {
        button(STICK, 0, true);
        button(STICK, 0, false);
        button(STICK, 5, true);

        assertEquals(List.of(), button(STICK, 5, false).orElseThrow().modifiers());
    }

    @Test
    void atMostThreeModifiersAreKept() {
        for (int b = 0; b < 4; b++) button(STICK, b, true);
        button(STICK, 9, true);

        assertEquals(3, button(STICK, 9, false).orElseThrow().modifiers().size());
    }

    @Test
    void aHatDirectionIsAPovTokenPickedWhenLetGo() {
        assertEquals(Optional.empty(), capture.hat(STICK, 0, DeviceHatEvent.UP));
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV1Up"), List.of())), capture.hat(STICK, 0, 0));
        capture.hat(STICK, 1, DeviceHatEvent.LEFT);
        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV2Left"), List.of())), capture.hat(STICK, 1, 0));
    }

    @Test
    void aDiagonalPicksTheFirstDirectionAndHoldsTheOther() {
        capture.hat(STICK, 0, DeviceHatEvent.UP | DeviceHatEvent.RIGHT);
        button(STICK, 0, true);

        assertEquals(List.of(input(STICK, "Joy_POV1Up"), input(STICK, "Joy_POV1Right")),
                button(STICK, 0, false).orElseThrow().modifiers());
    }

    @Test
    void aDiagonalLetGoPicksTheFirstDirectionWithTheOtherHeld() {
        capture.hat(STICK, 0, DeviceHatEvent.UP | DeviceHatEvent.RIGHT);

        assertEquals(Optional.of(new Captured(input(STICK, "Joy_POV1Up"), List.of(input(STICK, "Joy_POV1Right")))),
                capture.hat(STICK, 0, 0));
    }

    /** An axis is picked the moment it moves; a button held with it, let go afterwards, does not replace that pick. */
    @Test
    void aButtonLetGoAfterAnAxisPickPicksNothing() {
        capture.rest(STICK, GameAxis.RX, 0.0f);
        button(STICK, 0, true);

        assertEquals(Optional.of(new Captured(input(STICK, "Pos_Joy_RXAxis"), List.of(input(STICK, "Joy_1")))),
                capture.axis(STICK, GameAxis.RX, 0.8f));
        assertEquals(Optional.empty(), button(STICK, 0, false));
    }

    @Test
    void anUnpluggedControllerHoldsNothing() {
        button(STICK, 0, true);
        capture.release(STICK);
        button(THROTTLE, 0, true);

        assertEquals(List.of(), button(THROTTLE, 0, false).orElseThrow().modifiers());
    }

    @Test
    void anUnpluggedControllersPressPicksNothingLater() {
        button(STICK, 0, true);
        capture.release(STICK);

        assertEquals(Optional.empty(), button(STICK, 0, false));
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
        button("GamePad", 0, true);
        assertEquals(List.of(), button("GamePad", 0, false).orElseThrow().modifiers());
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
        ControllerCapture pad = new ControllerCapture();
        assertEquals(Optional.empty(), pad.gamepadHat("GamePad", 0, DeviceHatEvent.RIGHT));
        assertEquals(Optional.of(new Captured(input("GamePad", "GamePad_DPadRight"), List.of())),
                pad.gamepadHat("GamePad", 0, 0));
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

    @Test
    void aWholeAxisIsCapturedFromRestWithNoDirectionAndNoModifiers() {
        ControllerCapture capture = new ControllerCapture();
        capture.button("Stick", "Joy_1", true);

        assertTrue(capture.wholeAxis("Stick", GameAxis.Z, -1f).isEmpty(), "parked at one end");
        assertTrue(capture.wholeAxis("Stick", GameAxis.Z, -0.9f).isEmpty(), "a jitter");
        ControllerCapture.Captured captured = capture.wholeAxis("Stick", GameAxis.Z, -0.5f).orElseThrow();

        assertEquals("Joy_ZAxis", captured.input().key());
        assertTrue(captured.modifiers().isEmpty());
        assertTrue(capture.wholeAxis("Stick", GameAxis.Z, 0.5f).isEmpty(), "still moved");
    }
}
