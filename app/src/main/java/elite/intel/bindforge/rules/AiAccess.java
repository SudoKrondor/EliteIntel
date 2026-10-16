package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;

import java.util.Set;

/**
 * Whether VEGA can operate a control, as the bindings grid's AI ACCESS column shows it.
 * <p>
 * VEGA can only press keyboard keys. A control it uses that sits only on a controller looks like a perfectly
 * good binding - the game honours it, the stick works - and VEGA silently cannot use it. That is the case this
 * exists to make visible.
 * <p>
 * <strong>Only the controls VEGA uses are judged.</strong> A commander who flies on a stick has hundreds of
 * controller-only rows, and almost none of them matter to VEGA; flagging them all would bury the few that do.
 */
public final class AiAccess {

    public enum State {
        /**
         * VEGA never presses this control - nothing to say.
         */
        NOT_USED,
        /**
         * VEGA uses it and has a keyboard key to press.
         */
        READY,
        /**
         * VEGA uses it, it is bound, but to nothing VEGA can press.
         */
        CANT_PRESS,
        /**
         * VEGA uses it and nothing is bound at all.
         */
        NOT_BOUND
    }

    private final Set<String> vegaControls;

    /**
     * @param vegaControls the binding ids VEGA presses: its built-in commands' controls and every binding a
     *                     custom command taps
     */
    public AiAccess(Set<String> vegaControls) {
        this.vegaControls = Set.copyOf(vegaControls);
    }

    public State stateOf(String bindingId, ReadOnlyBindingSlots slots) {
        if (!vegaControls.contains(bindingId)) {
            return State.NOT_USED;
        }
        ReadOnlyBindingSlot primary = slots == null ? null : slots.primary();
        ReadOnlyBindingSlot secondary = slots == null ? null : slots.secondary();
        if (isKeyboardUsable(primary) || isKeyboardUsable(secondary)) {
            return State.READY;
        }
        return KeyBindingsParser.isBoundToNonKeyboardDeviceOnly(primary, secondary)
                ? State.CANT_PRESS
                : State.NOT_BOUND;
    }

    /**
     * True when neither slot is free, so a keyboard key can only go in by replacing what is there - the
     * commander's trade to make, never this application's.
     */
    public static boolean bothSlotsTaken(ReadOnlyBindingSlots slots) {
        return slots != null && KeyBindingsParser.isAssigned(slots.primary()) && KeyBindingsParser.isAssigned(slots.secondary());
    }

    private static boolean isKeyboardUsable(ReadOnlyBindingSlot slot) {
        return slot != null && slot.keyboardUsable();
    }
}
