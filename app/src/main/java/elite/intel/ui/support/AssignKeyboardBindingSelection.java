package elite.intel.ui.support;


import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.BindingSlotType;

/**
 * Dialog result for one selected slot. A {@code null} key means "clear slot";
 * localized display text is never passed to the writer.
 */
public record AssignKeyboardBindingSelection(BindingSlotType slotType, String key, BindingModifier modifier) {
}
