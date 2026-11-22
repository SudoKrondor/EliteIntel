package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingSlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A deliberate edit of one button slot - Primary or Secondary - made by the user: any device, up to three
 * modifiers on any device, and the slot's Hold flag. Clears the slot when {@link #input()} is {@code null}.
 * <p>
 * For {@link BindingsWriter#assignSlot(byte[], SlotAssignment)} only, which nothing automatic may call; the assistant's
 * own writes go through {@link KeyboardBindingEdit}.
 *
 * @param modifiers held with the input, in the order given; not significant to the game
 * @param hold      the slot's Hold flag, written as {@code <Hold Value="1" />}
 */
public record SlotAssignment(
        String bindingId,
        BindingSlotType slotType,
        BindingInput input,
        List<BindingInput> modifiers,
        boolean hold
) {
    /** The most modifiers the game keeps on one slot. */
    public static final int MAX_MODIFIERS = 3;

    public SlotAssignment {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(slotType, "slotType");
        modifiers = List.copyOf(modifiers == null ? List.of() : modifiers);
        if (input == null && (!modifiers.isEmpty() || hold)) {
            throw new IllegalArgumentException("Clearing a slot takes no modifiers and no Hold");
        }
        if (modifiers.size() > MAX_MODIFIERS) {
            throw new IllegalArgumentException("A slot takes at most " + MAX_MODIFIERS + " modifiers, not "
                    + modifiers.size());
        }
        List<BindingInput> seen = new ArrayList<>();
        for (BindingInput modifier : modifiers) {
            if (modifier.sameInputAs(input) || seen.stream().anyMatch(modifier::sameInputAs)) {
                throw new IllegalArgumentException("Modifier " + modifier + " repeats an input of the same slot");
            }
            seen.add(modifier);
        }
    }

    /** A request that empties the slot to {@code {NoDevice}}. */
    public static SlotAssignment clear(String bindingId, BindingSlotType slotType) {
        return new SlotAssignment(bindingId, slotType, null, List.of(), false);
    }

    public boolean clearsSlot() {
        return input == null;
    }
}
