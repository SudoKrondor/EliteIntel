package elite.intel.ui.dialog;

import elite.intel.bindforge.devices.ControllerCapture;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.ui.dialog.AssignKeyboardBindingDialog.SlotEdit;
import elite.intel.ui.support.AssignSlotSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers {@link SlotEdit#swap}: the slots exchange what they will hold, whole, and SAVE writes each one through the
 * user-edit path so neither write is refused for the other slot still holding that input.
 */
class AssignKeyboardBindingDialogSwapTest {

    /**
     * Which slot the parser says it read; a swap never looks at it.
     */
    private static final KeyBindingsParser.BindingSlotType PARSED_SLOT = KeyBindingsParser.BindingSlotType.PRIMARY;

    private static final BindingInput KEY_R = new BindingInput(BindingInput.KEYBOARD, "Key_R");
    private static final BindingInput JOY_5 = new BindingInput("Throttle", "Joy_5");
    private static final BindingInput LEFT_SHIFT = new BindingInput(BindingInput.KEYBOARD, "Key_LeftShift");

    @Test
    void theSlotsExchangeInputsModifiersAndHold() {
        SlotEdit primary = primary(keyboard(true));
        SlotEdit secondary = secondary(controller());

        SlotEdit.swap(primary, secondary);

        assertEquals(JOY_5, primary.inputAfter());
        assertEquals(List.of(), primary.modifiersAfter());
        assertFalse(primary.holdAfter());
        assertEquals(KEY_R, secondary.inputAfter());
        assertEquals(List.of(LEFT_SHIFT), secondary.modifiersAfter());
        assertTrue(secondary.holdAfter());
    }

    @Test
    void vegaKeepsTheKeyboardKeyWhicheverSlotItMovedTo() {
        SlotEdit primary = primary(keyboard(false));
        SlotEdit secondary = secondary(controller());

        SlotEdit.swap(primary, secondary);

        assertFalse(primary.keyboardAfter());
        assertTrue(secondary.keyboardAfter());
    }

    @Test
    void bothSlotsSaveThroughTheUserEditPathWithTheHoldTheyReceived() {
        SlotEdit primary = primary(keyboard(true));
        SlotEdit secondary = secondary(controller());

        SlotEdit.swap(primary, secondary);

        assertEquals(new AssignSlotSelection(BindingSlotType.PRIMARY, JOY_5, List.of(), false), primary.selection());
        assertEquals(new AssignSlotSelection(BindingSlotType.SECONDARY, KEY_R, List.of(LEFT_SHIFT), true),
                secondary.selection());
    }

    @Test
    void swappingWithAnEmptySlotClearsTheOneThatHeldTheInput() {
        SlotEdit primary = primary(keyboard(false));
        SlotEdit secondary = secondary(empty());

        SlotEdit.swap(primary, secondary);

        assertNull(primary.inputAfter());
        assertTrue(primary.changed());
        assertEquals(KEY_R, secondary.inputAfter());
        assertTrue(secondary.changed());
    }

    @Test
    void swappingTwiceLeavesNothingToSave() {
        SlotEdit primary = primary(keyboard(true));
        SlotEdit secondary = secondary(empty());

        SlotEdit.swap(primary, secondary);
        SlotEdit.swap(primary, secondary);

        assertFalse(primary.changed());
        assertFalse(secondary.changed());
    }

    @Test
    void aPickNotYetSavedMovesToo() {
        SlotEdit primary = primary(keyboard(false));
        SlotEdit secondary = secondary(empty());
        secondary.kind = AssignKeyboardBindingDialog.Kind.CONTROLLER;
        secondary.control = new ControllerCapture.Captured(JOY_5, List.of());

        SlotEdit.swap(primary, secondary);

        assertEquals(JOY_5, primary.inputAfter());
        assertEquals(KEY_R, secondary.inputAfter());
    }

    @Test
    void aSwapDropsTheRefusalShownOnACard() {
        SlotEdit primary = primary(keyboard(false));
        SlotEdit secondary = secondary(controller());
        primary.rejected = "Key_A - already in use";

        SlotEdit.swap(primary, secondary);

        assertNull(primary.rejected);
    }

    @Test
    void undoAfterASwapReturnsTheSlotToWhatItHeld() {
        SlotEdit primary = primary(keyboard(true));
        SlotEdit secondary = secondary(controller());

        SlotEdit.swap(primary, secondary);
        primary.undo();

        assertEquals(KEY_R, primary.inputAfter());
        assertTrue(primary.holdAfter());
        assertFalse(primary.changed());
    }

    private static SlotEdit primary(ReadOnlyBindingSlot slot) {
        return new SlotEdit(BindingSlotType.PRIMARY, slot, null);
    }

    private static SlotEdit secondary(ReadOnlyBindingSlot slot) {
        return new SlotEdit(BindingSlotType.SECONDARY, slot, null);
    }

    private static ReadOnlyBindingSlot keyboard(boolean hold) {
        return new ReadOnlyBindingSlot(KEY_R.device(), KEY_R.key(), new String[]{LEFT_SHIFT.key()},
                List.of(new BindingModifier(LEFT_SHIFT.device(), LEFT_SHIFT.key())), hold, PARSED_SLOT, true, true);
    }

    private static ReadOnlyBindingSlot controller() {
        return new ReadOnlyBindingSlot(JOY_5.device(), JOY_5.key(), new String[0], List.of(), false, PARSED_SLOT, false, false);
    }

    private static ReadOnlyBindingSlot empty() {
        return new ReadOnlyBindingSlot(BindingInput.NO_DEVICE, "", new String[0], List.of(), false, PARSED_SLOT, false, true);
    }
}
