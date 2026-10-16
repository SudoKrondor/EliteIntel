package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingModifier;

import java.util.*;

/**
 * Which other control already holds an input the capture dialog is about to assign - on any device, the
 * keyboard included.
 * <p>
 * <strong>The game's exact-chord model:</strong> two slots clash only when they hold the same inputs - the main
 * one and every modifier, in any order - with the same Hold. {@code Joy_6} alone and {@code Joy_6} with
 * {@code Joy_1} held are two controls. Two controls that are never live together - a ship control and an SRV
 * one - never clash ({@link BindingConflictRules#isSafeOverlap}), the same rule the keyboard check uses.
 * <p>
 * <strong>A clash blocks the assignment</strong> (Alan, 2026-10-15): the commander frees the input from the
 * control that holds it first. The writer would take it; the dialog does not offer it.
 */
public final class SlotInputConflicts {

    private SlotInputConflicts() {
    }

    /**
     * @param bindingId the control being edited - neither of its own slots counts
     * @param hold      the Hold the slot will carry, which is the one it has now
     * @return the first other control, by id, holding exactly these inputs; empty when they are free
     */
    public static Optional<String> holderOf(String bindingId, BindingInput input, List<BindingInput> modifiers,
                                            boolean hold, Map<String, ReadOnlyBindingSlots> slots) {
        if (input == null) return Optional.empty();
        Set<String> wanted = chordOf(input, modifiers);
        return slots.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(bindingId))
                .filter(entry -> holds(entry.getValue(), wanted, hold))
                .map(Map.Entry::getKey)
                .filter(other -> !BindingConflictRules.isSafeOverlap(bindingId, other))
                .sorted()
                .findFirst();
    }

    private static boolean holds(ReadOnlyBindingSlots slots, Set<String> wanted, boolean hold) {
        return slots != null && (holds(slots.primary(), wanted, hold) || holds(slots.secondary(), wanted, hold));
    }

    private static boolean holds(ReadOnlyBindingSlot slot, Set<String> wanted, boolean hold) {
        if (!KeyBindingsParser.isAssigned(slot) || slot.hold() != hold) return false;
        List<BindingInput> modifiers = slot.bindingModifiers().stream()
                .filter(m -> m.device() != null && m.key() != null && !m.key().isBlank())
                .map(SlotInputConflicts::asInput)
                .toList();
        return wanted.equals(chordOf(new BindingInput(slot.device(), slot.key()), modifiers));
    }

    private static BindingInput asInput(BindingModifier modifier) {
        return new BindingInput(modifier.device(), modifier.key());
    }

    // WHY ignoring case: the game matches device names and tokens without it - BindingInput.sameInputAs.
    private static Set<String> chordOf(BindingInput input, List<BindingInput> modifiers) {
        Set<String> chord = new HashSet<>();
        chord.add(identity(input));
        for (BindingInput modifier : modifiers) chord.add(identity(modifier));
        return chord;
    }

    private static String identity(BindingInput input) {
        return (input.device() + "\u0000" + input.key()).toLowerCase(Locale.ROOT);
    }
}
