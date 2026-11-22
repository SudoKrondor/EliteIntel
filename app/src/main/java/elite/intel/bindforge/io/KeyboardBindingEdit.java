package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingSlotType;

/**
 * Immutable request to assign one known keyboard key to one binding slot, or
 * clear that slot when {@link #key()} is {@code null}.
 * <p>
 * The writer edits a binds file's text held in memory - the edits not applied (Storage S5) - so no file stamp is
 * carried: {@code bindforge.storage.Edits} lets one change in at a time.
 */
public record KeyboardBindingEdit(
        String bindingId,
        BindingSlotType slotType,
        String key
) {
    /**
     * Clearing uses Elite Dangerous' built-in empty-slot representation:
     * {@code Device="{NoDevice}" Key=""}.
     */
    public boolean clearsSlot() {
        return key == null;
    }
}
