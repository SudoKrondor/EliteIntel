package elite.intel.ai.hands;

import elite.intel.ai.hands.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.ai.hands.KeyBindingsParser.ReadOnlyBindingSlots;

import java.util.*;

/**
 * Plans safe keyboard assignments for controls that have no keyboard binding.
 * <p>
 * This class is pure and file-free: it works from the parsed read-only slot map
 * and produces a list of intended edits plus a list of skips with reasons. The
 * caller is responsible for actually writing the edits (via {@link BindingsWriter})
 * and applying the draft.
 * <p>
 * Two invariants drive the design:
 * <ul>
 *   <li><b>Add, never replace.</b> An edit is only produced for an empty
 *       ({@code {NoDevice}}) slot. Controller (HOTAS/joystick/mouse) and existing
 *       keyboard assignments are never overwritten.</li>
 *   <li><b>Never collide.</b> A chord (key + optional modifier) is used at most
 *       once: not if it already appears anywhere in the file, and not twice within
 *       one batch.</li>
 * </ul>
 */
public class MissingBindingAutoAssigner {

    public enum SkipReason {
        /**
         * Both Primary and Secondary already hold a (non-keyboard) assignment.
         */
        BOTH_SLOTS_OCCUPIED,
        /**
         * No empty, safely-editable slot exists for this action.
         */
        NO_EDITABLE_SLOT,
        /**
         * The safe key pool was exhausted before this action could be assigned.
         */
        NO_FREE_KEY
    }

    public record PlannedEdit(String bindingId, BindingSlotType slotType, String key, BindingModifier modifier) {
    }

    public record SkippedBinding(String bindingId, SkipReason reason) {
    }

    public record Plan(List<PlannedEdit> edits, List<SkippedBinding> skipped) {
        public Plan {
            edits = List.copyOf(edits);
            skipped = List.copyOf(skipped);
        }
    }

    /**
     * Plans assignments for every unbound keyboard-capable action in the file,
     * in stable (case-insensitive bindingId) order.
     */
    public Plan planAll(Map<String, ReadOnlyBindingSlots> slots) {
        Set<SafeKeyboardKeys.Chord> occupied = new HashSet<>(occupiedChords(slots));
        List<PlannedEdit> edits = new ArrayList<>();
        List<SkippedBinding> skipped = new ArrayList<>();
        for (String bindingId : unboundTargets(slots)) {
            assignOne(bindingId, slots.get(bindingId), occupied, edits, skipped);
        }
        return new Plan(edits, skipped);
    }

    /**
     * True when this control already has a usable keyboard binding in either
     * slot. This is the single definition of "bound" shared with the UI, so the
     * Missing tab and {@link #planAll} always agree on what counts as missing.
     */
    public boolean isKeyboardBound(ReadOnlyBindingSlots binding) {
        return isKeyboardUsable(binding.primary()) || isKeyboardUsable(binding.secondary());
    }

    /**
     * Plans an assignment for a single binding (the per-row "auto fix" action).
     * Returns an empty plan if the binding is unknown or already keyboard-bound.
     */
    public Plan planOne(String bindingId, Map<String, ReadOnlyBindingSlots> slots) {
        List<PlannedEdit> edits = new ArrayList<>();
        List<SkippedBinding> skipped = new ArrayList<>();
        ReadOnlyBindingSlots binding = slots.get(bindingId);
        if (binding == null || isKeyboardBound(binding)) {
            return new Plan(edits, skipped);
        }
        Set<SafeKeyboardKeys.Chord> occupied = new HashSet<>(occupiedChords(slots));
        assignOne(bindingId, binding, occupied, edits, skipped);
        return new Plan(edits, skipped);
    }

    private void assignOne(
            String bindingId,
            ReadOnlyBindingSlots binding,
            Set<SafeKeyboardKeys.Chord> occupied,
            List<PlannedEdit> edits,
            List<SkippedBinding> skipped
    ) {
        BindingSlotType slotType = chooseWritableSlot(binding);
        if (slotType == null) {
            skipped.add(new SkippedBinding(bindingId, slotSkipReason(binding)));
            return;
        }
        SafeKeyboardKeys.Chord chord = firstFreeChord(occupied);
        if (chord == null) {
            skipped.add(new SkippedBinding(bindingId, SkipReason.NO_FREE_KEY));
            return;
        }
        occupied.add(chord);
        edits.add(new PlannedEdit(bindingId, slotType, chord.key(), chord.modifier()));
    }

    private List<String> unboundTargets(Map<String, ReadOnlyBindingSlots> slots) {
        List<String> targets = new ArrayList<>();
        for (Map.Entry<String, ReadOnlyBindingSlots> entry : slots.entrySet()) {
            if (!isKeyboardBound(entry.getValue())) {
                targets.add(entry.getKey());
            }
        }
        targets.sort(String.CASE_INSENSITIVE_ORDER);
        return targets;
    }

    private boolean isKeyboardUsable(ReadOnlyBindingSlot slot) {
        return slot != null && slot.keyboardUsable();
    }

    /**
     * Prefer the empty Primary slot; fall back to an empty Secondary.
     */
    private BindingSlotType chooseWritableSlot(ReadOnlyBindingSlots binding) {
        if (isWritableEmpty(binding.primary())) {
            return BindingSlotType.PRIMARY;
        }
        if (isWritableEmpty(binding.secondary())) {
            return BindingSlotType.SECONDARY;
        }
        return null;
    }

    private SkipReason slotSkipReason(ReadOnlyBindingSlots binding) {
        boolean primaryOccupied = isNonEmptyOccupied(binding.primary());
        boolean secondaryOccupied = isNonEmptyOccupied(binding.secondary());
        return primaryOccupied && secondaryOccupied
                ? SkipReason.BOTH_SLOTS_OCCUPIED
                : SkipReason.NO_EDITABLE_SLOT;
    }

    /**
     * A slot we can safely write to: present, and the canonical Elite empty slot.
     */
    private boolean isWritableEmpty(ReadOnlyBindingSlot slot) {
        return slot != null
                && "{NoDevice}".equals(slot.device())
                && (slot.key() == null || slot.key().isBlank());
    }

    private boolean isNonEmptyOccupied(ReadOnlyBindingSlot slot) {
        return slot != null && !isWritableEmpty(slot);
    }

    private SafeKeyboardKeys.Chord firstFreeChord(Set<SafeKeyboardKeys.Chord> occupied) {
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords()) {
            if (!occupied.contains(chord)) {
                return chord;
            }
        }
        return null;
    }

    /**
     * Collects every keyboard chord already present in the file. Only keyboard
     * assignments can collide with a keyboard chord; controller bindings are
     * irrelevant here. A chord is key + its single supported modifier (or none).
     */
    private Set<SafeKeyboardKeys.Chord> occupiedChords(Map<String, ReadOnlyBindingSlots> slots) {
        Set<SafeKeyboardKeys.Chord> occupied = new HashSet<>();
        for (ReadOnlyBindingSlots binding : slots.values()) {
            addChordIfKeyboard(binding.primary(), occupied);
            addChordIfKeyboard(binding.secondary(), occupied);
        }
        return occupied;
    }

    private void addChordIfKeyboard(ReadOnlyBindingSlot slot, Set<SafeKeyboardKeys.Chord> occupied) {
        if (slot == null || !"Keyboard".equals(slot.device())) {
            return;
        }
        String key = slot.key();
        if (key == null || key.isBlank() || "Key_".equals(key)) {
            return;
        }
        occupied.add(new SafeKeyboardKeys.Chord(key, singleSupportedModifier(slot)));
    }

    private BindingModifier singleSupportedModifier(ReadOnlyBindingSlot slot) {
        List<BindingModifier> modifiers = slot.bindingModifiers();
        if (modifiers.size() == 1 && modifiers.get(0).isSupportedKeyboardModifier()) {
            return modifiers.get(0);
        }
        return null;
    }
}
