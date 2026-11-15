package elite.intel.ai.hands;

import elite.intel.db.managers.GlobalSettingsManager;

import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_ENTER_SUPERCRUISE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_JUMP_TO_HYPERSPACE;
import static elite.intel.ai.hands.Bindings.GameCommand.BINDING_TOGGLE_FSD;

/**
 * Which game control engages the frame shift drive, by the commander's choice (Commander, Global Ship Settings).
 * <p>
 * By default supercruise and hyperspace are separate controls, so "supercruise" never jumps out of the system.
 * A commander who prefers the game's Toggle Frame Shift Drive gets that one control for both, and the game decides
 * which happens: with a system targeted it jumps, otherwise it enters supercruise. The app tracks none of that -
 * it only presses what the setting names.
 */
public final class FsdControls {

    private FsdControls() {
    }

    /** The control the supercruise command presses. */
    public static Bindings.GameCommand supercruise() {
        return supercruise(GlobalSettingsManager.getInstance().getUseFsdToggle());
    }

    /** The control the hyperspace command presses. */
    public static Bindings.GameCommand hyperspace() {
        return hyperspace(GlobalSettingsManager.getInstance().getUseFsdToggle());
    }

    /**
     * The control the commander's choice leaves unpressed, which therefore need not be bound: the toggle while it is
     * off, the separate Hyperspace control while it is on. Supercruise stays needed either way, because dropping
     * out of supercruise presses it.
     */
    public static String unusedControl() {
        return unusedControl(GlobalSettingsManager.getInstance().getUseFsdToggle());
    }

    static Bindings.GameCommand supercruise(boolean useToggle) {
        return useToggle ? BINDING_TOGGLE_FSD : BINDING_ENTER_SUPERCRUISE;
    }

    static Bindings.GameCommand hyperspace(boolean useToggle) {
        return useToggle ? BINDING_TOGGLE_FSD : BINDING_JUMP_TO_HYPERSPACE;
    }

    static String unusedControl(boolean useToggle) {
        return (useToggle ? BINDING_JUMP_TO_HYPERSPACE : BINDING_TOGGLE_FSD).getGameBinding();
    }
}
