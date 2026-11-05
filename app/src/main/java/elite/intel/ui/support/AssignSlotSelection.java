package elite.intel.ui.support;

import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingSlotType;

import java.util.List;

/**
 * An Assign Control result written through the user-edit path ({@code BindingsWriter.assignSlot}): a controller
 * input, or a keyboard key or a clear on a slot that holds a controller, which the keyboard path will not touch.
 * A {@code null} input clears the slot.
 *
 * @param hold the slot's Hold after the write: its own, or the one that came with an input swapped in
 */
public record AssignSlotSelection(BindingSlotType slotType, BindingInput input, List<BindingInput> modifiers,
                                  boolean hold)
        implements AssignSelection {

    public AssignSlotSelection {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }
}
