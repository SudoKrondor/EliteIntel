package elite.intel.ai.hands;

import java.util.*;

/**
 * Detects keyboard binding conflicts using Elite Dangerous's input-matching model.
 * <p>
 * ED matches a binding by its <strong>exact</strong> chord - the main key plus exactly its modifier
 * set. Holding extra modifiers does NOT trigger a binding that has fewer of them: bare {@code Key_6}
 * (galaxy map) and {@code Ctrl+Alt+Key_6} (pitch) coexist and fire distinctly. So two bindings
 * conflict only when they share the <strong>identical</strong> key-set, within the same active context.
 * <p>
 * (An earlier model treated a bare key as a subset that "swallowed" modified chords on that key.
 * In-game testing disproved it - the failure that suggested it was a stale-bindings state, where ED
 * had not re-read the {@code .binds}, not a real conflict.)
 * <p>
 * A binding's key-set is the unordered union of its main key and modifiers, because ED's
 * {@code <Primary>}/{@code <Modifier>} elements are positional only - "Ctrl+Y" is the same chord no
 * matter which key sits in which element. ("Primary" there names a position inside one chord, and is
 * not the Primary <em>slot</em> below.)
 * <p>
 * Every action has two binding slots, a Primary and a Secondary, and ED fires either. Both are
 * scanned: a chord in a Secondary slot collides exactly as one in a Primary does. See {@link SlotRef}.
 * <p>
 * Context filtering - mutually exclusive vehicle states (ship / SRV / on-foot) and sub-mode overlays
 * (camera, FSS, SAA, store, …) - is delegated to {@link BindingConflictRules}.
 * <p>
 * Pure and side-effect free, so it is unit-testable without the file watcher or database.
 */
public final class BindingConflictScanner {

    /**
     * One detected conflict between two actions, ordered so {@code actionA < actionB}.
     *
     * @param chord    the shared key-set both actions are bound to, in Elite's raw tokens
     *                 ({@code Key_W}, {@code Key_LeftControl}, ...). Kept raw so the domain stays
     *                 free of presentation; render it with {@link BindingChordSpeech} for the voice
     *                 warning, or {@code BindingSlotDisplayFormatter} for the Bindings tab.
     * @param blocking whether this conflict stops EliteIntel driving the game outright rather than merely
     *                 risking interference - see {@link BindingConflictRules#isBlocking}. A blocking conflict
     *                 is announced on every start, not once.
     */
    public record Conflict(String actionA, String actionB, Set<String> chord, String description, boolean blocking) {
    }

    /**
     * The conflict a candidate chord would create, naming the binding it collides with.
     */
    public record CandidateConflict(String otherBinding) {
    }

    /**
     * A ship action and its SRV ({@code _Buggy}) twin that are bound to <em>different</em> chords.
     * Not a conflict - the two never co-fire - but a soft suggestion to unify, since many players
     * prefer the same key in both vehicles. Ordered so {@code shipAction} is the ship variant.
     */
    public record Recommendation(String shipAction, String buggyAction) {
    }

    /**
     * One slot of one action - the unit a conflict scan actually compares.
     * <p>
     * WHY this exists: ED gives every action a Primary and a Secondary slot, and either can hold a
     * chord. Keying a scan by action name alone can only ever represent one of them, so a chord shared
     * between one action's Primary and another's Secondary was discarded before any rule ran - the scan
     * never saw it.
     * <p>
     * The action name remains the unit of <em>judgement</em>: {@link BindingConflictRules} asks about
     * actions, and {@link Conflict} reports actions. The slot only decides what gets compared.
     */
    record SlotRef(String action, KeyBindingsParser.BindingSlotType slot) {
    }

    /**
     * Orders slot entries by action, then slot, so pairing and output are deterministic.
     */
    private static final Comparator<Map.Entry<SlotRef, Set<String>>> BY_ACTION_THEN_SLOT =
            Comparator.<Map.Entry<SlotRef, Set<String>>, String>comparing(e -> e.getKey().action())
                    .thenComparing(e -> e.getKey().slot());

    private BindingConflictScanner() {
    }

    /**
     * Scans <em>both</em> slots of every keyboard binding for same-context duplicate chords.
     *
     * @param slots action name → its Primary/Secondary pair, as from
     *              {@link BindingsMonitor#getBindingSlots()}
     * @return all conflicts, each action pair reported once per shared chord, in deterministic order
     */
    public static List<Conflict> scanSlots(Map<String, KeyBindingsParser.BindingSlots> slots) {
        return scanSlotKeysets(toSlotKeysets(slots));
    }

    /**
     * Core algorithm over already-extracted key-sets, one per action. Package-private so it can be
     * exercised directly in tests without constructing {@link KeyBindingsParser.KeyBinding}s.
     * <p>
     * Equivalent to {@link #scanSlotKeysets} where every chord sits in a Primary slot.
     * <p>
     * WHY it still exists: <strong>no production caller</strong>. Around 45 tests were written against
     * an action-keyed map before the scan read both slots, and this reaches the real core for them.
     * Migrating those fixtures to slot maps would let this, {@link #recommendVehicleTwinsKeysets},
     * {@link #candidateConflict} and {@link #asPrimarySlots} all go.
     */
    static List<Conflict> scanKeysets(Map<String, Set<String>> keysets) {
        return scanSlotKeysets(asPrimarySlots(keysets));
    }

    /**
     * Core algorithm over key-sets keyed by {@link SlotRef}, so a chord in a Secondary slot is
     * compared like any other.
     * <p>
     * Two slots of the <em>same</em> action never conflict: a binding cannot compete with itself, and
     * holding one chord in both slots is how a player gives an action two ways to fire. An action pair
     * sharing a chord is reported once however many slot combinations produce it.
     */
    static List<Conflict> scanSlotKeysets(Map<SlotRef, Set<String>> keysets) {
        List<Conflict> conflicts = new ArrayList<>();
        Set<PairChord> reported = new HashSet<>();
        // Sorted by action, then slot, for deterministic pairing and output.
        List<Map.Entry<SlotRef, Set<String>>> entries = new ArrayList<>(keysets.entrySet());
        entries.sort(BY_ACTION_THEN_SLOT);

        for (int i = 0; i < entries.size(); i++) {
            SlotRef refA = entries.get(i).getKey();
            Set<String> ksA = entries.get(i).getValue();
            for (int j = i + 1; j < entries.size(); j++) {
                SlotRef refB = entries.get(j).getKey();
                Set<String> ksB = entries.get(j).getValue();

                String a = refA.action();
                String b = refB.action();
                if (a.equals(b)) {
                    continue; // one action's own two slots - not a competitor
                }
                if (!ksA.equals(ksB)) {
                    continue; // ED matches the exact chord; only identical chords clash
                }
                if (BindingConflictRules.isSafeOverlap(a, b)) {
                    continue; // different vehicle state or a sub-mode overlay → never co-fire
                }
                Set<String> chord = Set.copyOf(ksA);
                if (!reported.add(new PairChord(a, b, chord))) {
                    continue; // already reported for this pair on this chord, from another slot pairing
                }
                conflicts.add(new Conflict(a, b, chord, BindingConflictRules.describe(a, b),
                        BindingConflictRules.isBlocking(a, b)));
            }
        }
        return conflicts;
    }

    /**
     * An action pair on one chord, so the same clash found through two slot combinations is reported
     * once.
     */
    private record PairChord(String actionA, String actionB, Set<String> chord) {
    }

    /**
     * Suggests ship/SRV control twins that are bound to different chords, so the editor can nudge the
     * player to unify them. Only twins where <em>both</em> halves are bound qualify; an unbound twin
     * is a missing-binding concern handled elsewhere, not a recommendation.
     *
     * @param slots action name → its Primary/Secondary pair
     * @return one recommendation per mismatched twin pair, in deterministic order
     */
    public static List<Recommendation> recommendVehicleTwinsFromSlots(
            Map<String, KeyBindingsParser.BindingSlots> slots) {
        return recommendVehicleTwinsFromSlotKeysets(toSlotKeysets(slots));
    }

    /**
     * Keyset-based core, so it can be tested without KeyBindings. Test-only - see {@link #scanKeysets}.
     */
    static List<Recommendation> recommendVehicleTwinsKeysets(Map<String, Set<String>> keysets) {
        return recommendVehicleTwinsFromSlotKeysets(asPrimarySlots(keysets));
    }

    /**
     * Slot-aware core. Twins count as unified when they share <em>any</em> chord: a player who put the
     * same key on the ship's Primary and the SRV's Secondary has already done the thing this would
     * suggest, and nagging them about it is how a useful nudge turns into noise.
     */
    static List<Recommendation> recommendVehicleTwinsFromSlotKeysets(Map<SlotRef, Set<String>> keysets) {
        Map<String, Set<Set<String>>> chordsByAction = new TreeMap<>();
        for (Map.Entry<SlotRef, Set<String>> e : keysets.entrySet()) {
            chordsByAction.computeIfAbsent(e.getKey().action(), k -> new HashSet<>()).add(e.getValue());
        }

        List<Recommendation> recommendations = new ArrayList<>();
        for (Map.Entry<String, Set<Set<String>>> entry : chordsByAction.entrySet()) {
            String buggy = entry.getKey();
            String ship = BindingConflictRules.shipTwinOf(buggy);
            if (ship == null) {
                continue; // not an SRV variant
            }
            Set<Set<String>> shipChords = chordsByAction.get(ship);
            if (shipChords == null) {
                continue; // ship twin unbound → missing-binding concern, not a recommendation
            }
            if (Collections.disjoint(shipChords, entry.getValue())) {
                recommendations.add(new Recommendation(ship, buggy));
            }
        }
        return recommendations;
    }

    /**
     * Reports the binding (if any) whose chord is identical to the candidate ({@code key} +
     * {@code modifiers}) for {@code bindingId} within the same context, or {@code null} if the
     * candidate is free. Used by the editor save-guard and the live keyboard widget.
     * <p>
     * Judged against <em>both</em> slots of every existing binding: a chord sitting in some other
     * action's Secondary slot is taken, and a save-guard that cannot see it waves the player through
     * to a clash the game will honour. The binding's own other slot is never a self-conflict.
     */
    public static CandidateConflict candidateConflictInSlots(
            String bindingId, String key, Collection<String> modifiers,
            Map<String, KeyBindingsParser.BindingSlots> existingSlots) {
        return candidateConflictInSlotKeysets(
                bindingId, chordOf(key, modifiers), toSlotKeysets(existingSlots));
    }

    /**
     * Keyset-based core, so it can be tested without KeyBindings. Test-only - see {@link #scanKeysets}.
     */
    static CandidateConflict candidateConflict(String bindingId, Set<String> candidate, Map<String, Set<String>> existing) {
        return candidateConflictInSlotKeysets(bindingId, candidate, asPrimarySlots(existing));
    }

    /**
     * Slot-aware core. Both slots of the binding being edited are skipped: an action never conflicts
     * with itself, whichever slot the chord already sits in.
     */
    static CandidateConflict candidateConflictInSlotKeysets(
            String bindingId, Set<String> candidate, Map<SlotRef, Set<String>> existing) {
        if (candidate.isEmpty()) {
            return null; // a blank or unbound chord collides with nothing
        }
        // Sorted so the binding named back is stable when a chord is taken more than once.
        List<Map.Entry<SlotRef, Set<String>>> entries = new ArrayList<>(existing.entrySet());
        entries.sort(BY_ACTION_THEN_SLOT);

        for (Map.Entry<SlotRef, Set<String>> e : entries) {
            String other = e.getKey().action();
            if (other.equals(bindingId)) {
                continue; // a binding never conflicts with its own other slot
            }
            if (!candidate.equals(e.getValue())) {
                continue; // exact chord match only
            }
            if (BindingConflictRules.isSafeOverlap(bindingId, other)) {
                continue;
            }
            return new CandidateConflict(other);
        }
        return null;
    }

    /**
     * The key-set one slot contributes to a scan - its main key plus any modifiers. Tolerates a blank
     * key and the {@code Key_} placeholder Elite writes for an empty slot, both of which are "unbound"
     * rather than a chord.
     */
    static Set<String> chordOf(String key, Collection<String> modifiers) {
        if (key == null || key.isBlank() || key.equals("Key_")) {
            return Set.of();
        }
        Set<String> keys = new HashSet<>();
        keys.add(key);
        if (modifiers != null) {
            for (String modifier : modifiers) {
                if (modifier != null && !modifier.isBlank()) {
                    keys.add(modifier);
                }
            }
        }
        return keys;
    }

    /**
     * The chord one parsed slot holds, or an empty set when the slot is empty.
     * <p>
     * Public because a caller holding a {@link Conflict} may need to find which of an action's two
     * slots the scan actually matched on. Since the scan reads both, "the Primary if it holds a key"
     * is no longer that slot, and telling the commander to move the wrong chord is worse than telling
     * them nothing. Taking the slot rather than its parts also saves each caller a hand-rolled unpack
     * and the array copy {@code modifiers()} returns.
     */
    public static Set<String> chordOf(KeyBindingsParser.ReadOnlyBindingSlot slot) {
        return slot == null ? Set.of() : chordOf(slot.key(), Arrays.asList(slot.modifiers()));
    }

    /**
     * The full set of keys a binding's chord requires held: its main key plus all modifiers.
     */
    static Set<String> keysetOf(KeyBindingsParser.KeyBinding kb) {
        if (kb == null) {
            return Set.of();
        }
        return chordOf(kb.key, kb.modifiers == null ? null : Arrays.asList(kb.modifiers));
    }

    /**
     * Key-sets for both slots of every action. A slot with nothing in it contributes nothing.
     */
    private static Map<SlotRef, Set<String>> toSlotKeysets(Map<String, KeyBindingsParser.BindingSlots> slots) {
        Map<SlotRef, Set<String>> keysets = new LinkedHashMap<>();
        for (Map.Entry<String, KeyBindingsParser.BindingSlots> e : slots.entrySet()) {
            KeyBindingsParser.BindingSlots pair = e.getValue();
            putIfBound(keysets, e.getKey(), KeyBindingsParser.BindingSlotType.PRIMARY, pair.primary());
            putIfBound(keysets, e.getKey(), KeyBindingsParser.BindingSlotType.SECONDARY, pair.secondary());
        }
        return keysets;
    }

    private static void putIfBound(Map<SlotRef, Set<String>> keysets, String action,
                                   KeyBindingsParser.BindingSlotType slot,
                                   KeyBindingsParser.KeyBinding binding) {
        Set<String> keyset = keysetOf(binding);
        if (!keyset.isEmpty()) {
            keysets.put(new SlotRef(action, slot), keyset);
        }
    }

    /**
     * Reads an action-keyed map as one Primary slot per action, so the test-only entry points and the
     * slot-aware core share a single algorithm rather than two that can drift apart.
     * See {@link #scanKeysets} for why they are still here.
     */
    private static Map<SlotRef, Set<String>> asPrimarySlots(Map<String, Set<String>> keysets) {
        Map<SlotRef, Set<String>> slots = new LinkedHashMap<>();
        if (keysets != null) {
            for (Map.Entry<String, Set<String>> e : keysets.entrySet()) {
                slots.put(new SlotRef(e.getKey(), KeyBindingsParser.BindingSlotType.PRIMARY), e.getValue());
            }
        }
        return slots;
    }
}
