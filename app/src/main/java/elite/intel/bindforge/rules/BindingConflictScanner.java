package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;

import java.util.*;

/**
 * Detects keyboard binding conflicts using Elite Dangerous's input-matching model.
 * <p>
 * ED matches a binding by its <strong>exact</strong> chord - the main key plus exactly its modifier
 * set. Holding extra modifiers does NOT trigger a binding that has fewer of them: bare {@code Key_6}
 * (galaxy map) and {@code Ctrl+Alt+Key_6} (pitch) coexist and fire distinctly. So two bindings
 * conflict only when they share the <strong>identical</strong> key-set, within the same active context.
 * <p>
 * One exception, the <em>modifier shadow</em>: a chord whose modifier key is also bound
 * <strong>on its own</strong> to another action in the same context. A modifier goes down before the
 * rest of the chord - from the commander's hand and from EliteIntel alike - and for that moment it is
 * the whole chord, so the bare binding fires. Reported from a support bundle of 2026-09-30: Galaxy Map
 * on {@code Ctrl+Shift+J} with UI Focus (hold) on bare {@code Left Shift}, so every attempt to open the
 * map pulled the view back toward the panels and the map never opened. Only the six keyboard modifier
 * keys are judged this way; a bare <em>main</em> key and a modified chord on it still coexist.
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
     * @param chordA   the key-set {@code actionA} is bound to where the conflict happens, in Elite's raw
     *                 tokens ({@code Key_W}, {@code Key_LeftControl}, ...). Kept raw so the domain stays
     *                 free of presentation; render it with {@link BindingChordSpeech} for the voice
     *                 warning, or {@code BindingSlotDisplayFormatter} for the Bindings tab.
     * @param chordB   the same for {@code actionB}. Equal to {@code chordA} for an identical-chord clash;
     *                 for a modifier shadow one of the two is the bare modifier and the other the chord
     *                 that holds it, so each side has to name its own - the Bindings tab finds the slot to
     *                 point at by matching it.
     * @param blocking whether this conflict stops EliteIntel driving the game outright rather than merely
     *                 risking interference - see {@link BindingConflictRules#isBlocking}. A blocking conflict
     *                 is announced on every start, not once.
     */
    public record Conflict(String actionA, Set<String> chordA, String actionB, Set<String> chordB,
                           String description, boolean blocking) {

        /**
         * The chord to name when announcing this conflict: the one both share, or for a modifier shadow
         * the full combination - "Left Control plus Left Shift plus J" says where to look, a bare
         * "Left Shift" does not.
         */
        public Set<String> chord() {
            return chordA.size() >= chordB.size() ? chordA : chordB;
        }
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
     * <p>
     * {@code hold} is the slot's {@code <Hold Value="1"/>}: Elite fires it on a long press instead of a tap, and
     * shows it in its own controls screen as {@code [1](HOLD)}. A press and a hold on one key are two separate
     * controls - Use Health Pack is a long press on the key whose tap selects the primary weapon.
     */
    record SlotRef(String action, KeyBindingsParser.BindingSlotType slot, boolean hold) {

        SlotRef(String action, KeyBindingsParser.BindingSlotType slot) {
            this(action, slot, false);
        }
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
     * Scans <em>both</em> slots of every keyboard binding for same-context duplicate chords and
     * modifier shadows.
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
                boolean sameChord = ksA.equals(ksB);
                if (sameChord && refA.hold() != refB.hold()) {
                    continue; // a tap and a long press on one key are two controls, not a clash
                }
                String shadowed = sameChord ? null : shadowedModifier(ksA, ksB);
                if (!sameChord && shadowed == null) {
                    continue; // ED matches the exact chord; only identical chords or a shadowed modifier clash
                }
                if (BindingConflictRules.isSafeOverlap(a, b)) {
                    continue; // different vehicle state or a sub-mode overlay → never co-fire
                }
                Set<String> chordA = Set.copyOf(ksA);
                Set<String> chordB = Set.copyOf(ksB);
                Set<String> pairChord = chordA.size() >= chordB.size() ? chordA : chordB;
                if (!reported.add(new PairChord(a, b, pairChord))) {
                    continue; // already reported for this pair on this chord, from another slot pairing
                }
                String description;
                if (sameChord) {
                    description = BindingConflictRules.describe(a, b);
                } else if (chordA == pairChord) {
                    description = BindingConflictRules.describeModifierShadow(a, b, shadowed);
                } else {
                    description = BindingConflictRules.describeModifierShadow(b, a, shadowed);
                }
                // A shadow is never blocking: the blocking families are identical chords on a key the
                // interface walk taps, which is a different failure with its own every-start warning.
                conflicts.add(new Conflict(a, chordA, b, chordB, description,
                        sameChord && BindingConflictRules.isBlocking(a, b)));
            }
        }
        return conflicts;
    }

    /**
     * The modifier key one of the two key-sets holds as part of a chord while the other binds it on its
     * own, or {@code null} when neither does. Symmetric in its arguments.
     * <p>
     * Only the keyboard modifier keys qualify ({@link BindingModifier#isSupportedKeyboardModifier}): they
     * are what goes down first and stands alone for a moment. A bare main key against a modified chord on
     * that key is the case Elite's exact matching keeps apart - see the class comment.
     */
    static String shadowedModifier(Set<String> ksA, Set<String> ksB) {
        if (ksA.size() == 1 && ksB.size() > 1) {
            return bareModifierHeldIn(ksA, ksB);
        }
        if (ksB.size() == 1 && ksA.size() > 1) {
            return bareModifierHeldIn(ksB, ksA);
        }
        return null;
    }

    private static String bareModifierHeldIn(Set<String> bare, Set<String> chord) {
        String key = bare.iterator().next();
        return BindingModifier.isSupportedKeyboardModifier("Keyboard", key) && chord.contains(key) ? key : null;
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
     * {@code modifiers}) for {@code bindingId} within the same context, or that shadows it through a
     * modifier (see the class comment), or {@code null} if the candidate is free. Used by the editor save-guard and the live keyboard widget.
     * <p>
     * Judged against <em>both</em> slots of every existing binding: a chord sitting in some other
     * action's Secondary slot is taken, and a save-guard that cannot see it waves the player through
     * to a clash the game will honour. The binding's own other slot is never a self-conflict.
     */
    public static CandidateConflict candidateConflictInSlots(
            String bindingId, BindingSlotType slotType, String key, Collection<String> modifiers,
            Map<String, KeyBindingsParser.BindingSlots> existingSlots) {
        return candidateConflictInSlotKeysets(bindingId, chordOf(key, modifiers),
                isHoldSlot(existingSlots.get(bindingId), slotType), toSlotKeysets(existingSlots));
    }

    /**
     * Whether the slot being edited is a long press. A new key keeps the slot's {@code <Hold>} flag
     * ({@code BindingsWriter} writes it back), so the candidate is judged as the press or hold it will be.
     */
    private static boolean isHoldSlot(KeyBindingsParser.BindingSlots slots, BindingSlotType slotType) {
        if (slots == null || slotType == null) return false;
        KeyBindingsParser.KeyBinding binding =
                slotType == BindingSlotType.SECONDARY ? slots.secondary() : slots.primary();
        return binding != null && binding.hold;
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
        return candidateConflictInSlotKeysets(bindingId, candidate, false, existing);
    }

    static CandidateConflict candidateConflictInSlotKeysets(
            String bindingId, Set<String> candidate, boolean candidateHold, Map<SlotRef, Set<String>> existing) {
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
            boolean sameChord = candidate.equals(e.getValue());
            if (!sameChord && shadowedModifier(candidate, e.getValue()) == null) {
                continue; // exact chord match, or a modifier one side binds on its own
            }
            if (sameChord && candidateHold != e.getKey().hold()) {
                continue; // a tap and a long press on one key are two controls
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
            keysets.put(new SlotRef(action, slot, binding.hold), keyset);
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
