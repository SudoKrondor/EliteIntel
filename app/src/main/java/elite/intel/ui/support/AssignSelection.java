package elite.intel.ui.support;

import elite.intel.bindforge.model.BindingSlotType;

/**
 * What the Assign Control dialog hands back: a keyboard key for the keyboard path, or any input for the user-edit
 * path. Which one decides which writer method saves it.
 */
public sealed interface AssignSelection permits AssignKeyboardBindingSelection, AssignSlotSelection {

    BindingSlotType slotType();
}
