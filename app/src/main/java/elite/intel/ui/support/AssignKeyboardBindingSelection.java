package elite.intel.ui.support;


import elite.intel.ai.hands.BindingModifier;
import elite.intel.ai.hands.BindingSlotType;

import java.util.List;

/**
 * Dialog result for one selected slot. A {@code null} key means "clear slot";
 * localized display text is never passed to the writer.
 * <p>
 * {@code modifiers} carries the full captured chord in press order (e.g.
 * Left&nbsp;Ctrl + Left&nbsp;Shift); an empty list means an unmodified key.
 */
public record AssignKeyboardBindingSelection(BindingSlotType slotType, String key, List<BindingModifier> modifiers) {
    public AssignKeyboardBindingSelection {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }
}
