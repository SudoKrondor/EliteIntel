package elite.intel.ui.dialog;

import elite.intel.bindforge.devices.CaptureControllers;
import elite.intel.devices.model.GameAxis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ControllerInputsPanelTest {

    @Test
    void gamepadButtonsUseEliteSemanticTokensAndHideSystemButtons() {
        CaptureControllers.Controller controller =
                new CaptureControllers.Controller("GamePad", 1, "Xbox", 11, 1, List.of(), true);

        assertEquals(List.of(
                "GamePad_FaceDown", "GamePad_FaceRight", "GamePad_FaceLeft", "GamePad_FaceUp",
                "GamePad_LBumper", "GamePad_RBumper", "GamePad_Back",
                "GamePad_LThumb", "GamePad_RThumb"), ControllerInputsPanel.buttonTokens(controller));
    }

    @Test
    void gamepadHatUsesDPadTokens() {
        CaptureControllers.Controller controller =
                new CaptureControllers.Controller("GamePad", 1, "Xbox", 0, 1, List.of(), true);

        assertEquals(List.of("GamePad_DPadUp", "GamePad_DPadRight", "GamePad_DPadDown", "GamePad_DPadLeft"),
                ControllerInputsPanel.hatTokens(controller));
    }

    @Test
    void anOrdinaryControllerListsPlainButtonsAndPovHats() {
        CaptureControllers.Controller controller =
                new CaptureControllers.Controller("Stick", 1, "Stick", 3, 1, List.of(), false);

        assertEquals(List.of("Joy_1", "Joy_2", "Joy_3"), ControllerInputsPanel.buttonTokens(controller));
        assertEquals(List.of("Joy_POV1Up", "Joy_POV1Right", "Joy_POV1Down", "Joy_POV1Left"),
                ControllerInputsPanel.hatTokens(controller));
    }

    /** E2 (Alan, 2026-11-10): a − and a + cell for every axis, in the game's order. */
    @Test
    void axisDirectionsAreAMinusAndAPlusCellPerAxis() {
        CaptureControllers.Controller controller =
                new CaptureControllers.Controller("Stick", 1, "Stick", 0, 0, List.of(GameAxis.X, GameAxis.RZ), false);

        assertEquals(List.of("Neg_Joy_XAxis", "Pos_Joy_XAxis", "Neg_Joy_RZAxis", "Pos_Joy_RZAxis"),
                ControllerInputsPanel.axisTokens(controller));
    }

    /** A GamePad's page shows one cell per axis, for show only: its stick tokens are not measured (E7). */
    @Test
    void aGamepadKeepsOneCellPerAxis() {
        CaptureControllers.Controller controller =
                new CaptureControllers.Controller("GamePad", 1, "Xbox", 0, 0, List.of(GameAxis.X, GameAxis.Z), true);

        assertEquals(List.of("Joy_XAxis", "Joy_ZAxis"), ControllerInputsPanel.axisTokens(controller));
    }

    @Test
    void aDirectionIsItsOwnCell() {
        assertEquals("Pos_Joy_XAxis", ControllerInputsPanel.displayToken("Pos_Joy_XAxis", false));
        assertEquals("Neg_Joy_XAxis", ControllerInputsPanel.displayToken("Neg_Joy_XAxis", false));
        assertEquals(List.of("Neg_Joy_XAxis"), ControllerInputsPanel.bindingTokens("Neg_Joy_XAxis", false));
    }

    @Test
    void onAGamepadADirectionShowsOnItsAxisCell() {
        assertEquals("Joy_XAxis", ControllerInputsPanel.displayToken("Pos_Joy_XAxis", true));
        assertEquals("Joy_XAxis", ControllerInputsPanel.displayToken("Neg_Joy_XAxis", true));
    }

    @Test
    void aGamepadTriggerTokenShowsOnTheAxisItIsReportedAs() {
        assertEquals("Joy_ZAxis", ControllerInputsPanel.displayToken("Pos_GamePad_LTrigger", true));
        assertEquals("Joy_RZAxis", ControllerInputsPanel.displayToken("Pos_GamePad_RTrigger", true));
    }

    /** A direction says its axis's label with − or +, unless a label of its own is saved. */
    @Test
    void aDirectionCellNamesItsAxisAndSign() {
        Map<String, String> labels = Map.of("Joy_RZAxis", "Twist", "Pos_Joy_YAxis", "Nose down");

        assertEquals("Twist \u2212", ControllerInputsPanel.cellText("Neg_Joy_RZAxis", t -> Optional.ofNullable(labels.get(t))));
        assertEquals("Twist +", ControllerInputsPanel.cellText("Pos_Joy_RZAxis", t -> Optional.ofNullable(labels.get(t))));
        assertEquals("Nose down", ControllerInputsPanel.cellText("Pos_Joy_YAxis", t -> Optional.ofNullable(labels.get(t))));
        assertEquals("Joy_XAxis +", ControllerInputsPanel.cellText("Pos_Joy_XAxis", t -> Optional.empty()));
        assertEquals("Joy_5", ControllerInputsPanel.cellText("Joy_5", t -> Optional.empty()));
    }

    @Test
    void anAxisCellClashesOnEitherOfItsDirections() {
        assertEquals(List.of("Neg_Joy_XAxis", "Pos_Joy_XAxis"), ControllerInputsPanel.bindingTokens("Joy_XAxis", false));
        assertEquals(List.of("Neg_Joy_ZAxis", "Pos_Joy_ZAxis"), ControllerInputsPanel.bindingTokens("Joy_ZAxis", false));
    }

    @Test
    void aGamepadTriggerCellClashesOnItsTriggerToken() {
        assertEquals(List.of("Pos_GamePad_LTrigger"), ControllerInputsPanel.bindingTokens("Joy_ZAxis", true));
        assertEquals(List.of("Pos_GamePad_RTrigger"), ControllerInputsPanel.bindingTokens("Joy_RZAxis", true));
        assertEquals(List.of("Neg_Joy_XAxis", "Pos_Joy_XAxis"), ControllerInputsPanel.bindingTokens("Joy_XAxis", true));
    }

    @Test
    void aButtonOrHatCellClashesOnItsOwnToken() {
        assertEquals(List.of("Joy_5"), ControllerInputsPanel.bindingTokens("Joy_5", false));
        assertEquals(List.of("GamePad_DPadUp"), ControllerInputsPanel.bindingTokens("GamePad_DPadUp", true));
    }

    @Test
    void otherTokensShowOnTheirOwnCell() {
        assertEquals("Joy_5", ControllerInputsPanel.displayToken("Joy_5", false));
        assertEquals("GamePad_LBumper", ControllerInputsPanel.displayToken("GamePad_LBumper", true));
        assertNull(ControllerInputsPanel.displayToken(null, false));
    }
}
