package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ENTER_SUPERCRUISE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_JUMP_TO_HYPERSPACE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TOGGLE_FSD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FsdControlsTest {

    @Test
    void offPressesEachCommandsOwnControl() {
        assertEquals(BINDING_ENTER_SUPERCRUISE, FsdControls.supercruise(false));
        assertEquals(BINDING_JUMP_TO_HYPERSPACE, FsdControls.hyperspace(false));
        assertEquals("HyperSuperCombination", FsdControls.unusedControl(false));
    }

    @Test
    void onPressesTheGamesToggleForBoth() {
        assertEquals(BINDING_TOGGLE_FSD, FsdControls.supercruise(true));
        assertEquals(BINDING_TOGGLE_FSD, FsdControls.hyperspace(true));
        assertEquals("Hyperspace", FsdControls.unusedControl(true));
    }

    /** Supercruise stays required with the toggle on: dropping out of supercruise presses it. */
    @Test
    void supercruiseIsNeverTheUnusedControl() {
        assertNotEquals("Supercruise", FsdControls.unusedControl(true));
        assertNotEquals("Supercruise", FsdControls.unusedControl(false));
    }
}
