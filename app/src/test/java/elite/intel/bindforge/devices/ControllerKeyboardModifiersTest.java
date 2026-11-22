package elite.intel.bindforge.devices;

import elite.intel.bindforge.devices.ControllerKeyboardModifiers.Key;
import elite.intel.bindforge.model.BindingInput;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the three keyboard keys a controller input may be held with (E5): a tick adds the left key, a key already
 * in the slot keeps its side, and the controller's own modifiers are never touched.
 */
class ControllerKeyboardModifiersTest {

    private static final BindingInput JOY_3 = new BindingInput("VKB-Stick", "Joy_3");
    private static final BindingInput LEFT_SHIFT = new BindingInput(BindingInput.KEYBOARD, "Key_LeftShift");
    private static final BindingInput RIGHT_SHIFT = new BindingInput(BindingInput.KEYBOARD, "Key_RightShift");
    private static final BindingInput LEFT_CTRL = new BindingInput(BindingInput.KEYBOARD, "Key_LeftControl");
    private static final BindingInput LEFT_ALT = new BindingInput(BindingInput.KEYBOARD, "Key_LeftAlt");

    @Test
    void aTickedKeyIsAddedOnTheLeftAfterTheControllersOwnModifiers() {
        assertEquals(List.of(JOY_3, LEFT_SHIFT, LEFT_ALT),
                ControllerKeyboardModifiers.withKeys(List.of(JOY_3), EnumSet.of(Key.SHIFT, Key.ALT)));
    }

    @Test
    void aKeyAlreadyInTheSlotKeepsItsSideWhileItsBoxStaysTicked() {
        assertEquals(List.of(RIGHT_SHIFT, LEFT_CTRL),
                ControllerKeyboardModifiers.withKeys(List.of(RIGHT_SHIFT), EnumSet.of(Key.SHIFT, Key.CTRL)));
    }

    @Test
    void clearingABoxTakesTheKeyAwayOnEitherSideAndLeavesTheControllerModifiers() {
        assertEquals(List.of(JOY_3),
                ControllerKeyboardModifiers.withKeys(List.of(JOY_3, RIGHT_SHIFT), EnumSet.noneOf(Key.class)));
    }

    @Test
    void eitherSideCountsAsTheKeyHeld() {
        assertEquals(Set.of(Key.SHIFT, Key.CTRL),
                ControllerKeyboardModifiers.heldIn(List.of(JOY_3, RIGHT_SHIFT, LEFT_CTRL)));
    }

    @Test
    void aControllerButtonIsNeverTakenForAKeyboardKey() {
        BindingInput notAKey = new BindingInput("VKB-Stick", "Key_LeftShift");
        assertEquals(Set.of(), ControllerKeyboardModifiers.heldIn(List.of(notAKey)));
        assertEquals(List.of(notAKey), ControllerKeyboardModifiers.withKeys(List.of(notAKey), EnumSet.noneOf(Key.class)));
    }
}
