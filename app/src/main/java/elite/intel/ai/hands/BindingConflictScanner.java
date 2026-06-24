package elite.intel.ai.hands;

import java.util.*;

/**
 * Detects keyboard binding conflicts using Elite Dangerous's input-matching model.
 * <p>
 * ED matches a binding by its <strong>exact</strong> chord — the main key plus exactly its modifier
 * set. Holding extra modifiers does NOT trigger a binding that has fewer of them: bare {@code Key_6}
 * (galaxy map) and {@code Ctrl+Alt+Key_6} (pitch) coexist and fire distinctly. So two bindings
 * conflict only when they share the <strong>identical</strong> key-set, within the same active context.
 * <p>
 * (An earlier model treated a bare key as a subset that "swallowed" modified chords on that key.
 * In-game testing disproved it — the failure that suggested it was a stale-bindings state, where ED
 * had not re-read the {@code .binds}, not a real conflict.)
 * <p>
 * A binding's key-set is the unordered union of its primary key and modifiers, because ED's
 * {@code <Primary>}/{@code <Modifier>} slots are positional only — "Ctrl+Y" is the same chord no
 * matter which key sits in which slot.
 * <p>
 * Context filtering — mutually exclusive vehicle states (ship / SRV / on-foot) and sub-mode overlays
 * (camera, FSS, SAA, store, …) — is delegated to {@link BindingConflictRules}.
 * <p>
 * Pure and side-effect free, so it is unit-testable without the file watcher or database.
 */
public final class BindingConflictScanner {

    /**
     * One detected conflict between two actions, ordered so {@code actionA < actionB}.
     */
    public record Conflict(String actionA, String actionB, String description) {
    }

    /**
     * The conflict a candidate chord would create, naming the binding it collides with.
     */
    public record CandidateConflict(String otherBinding) {
    }

    private BindingConflictScanner() {
    }

    /**
     * Scans every keyboard binding for same-context duplicate chords.
     *
     * @param bindings action name → parsed binding, as from {@code BindingsMonitor.getBindings()}
     * @return all conflicts, each reported once, in deterministic order
     */
    public static List<Conflict> scan(Map<String, KeyBindingsParser.KeyBinding> bindings) {
        return scanKeysets(toKeysets(bindings));
    }

    /**
     * Core algorithm over already-extracted key-sets. Package-private so it can be exercised
     * directly in tests without constructing {@link KeyBindingsParser.KeyBinding}s.
     */
    static List<Conflict> scanKeysets(Map<String, Set<String>> keysets) {
        List<Conflict> conflicts = new ArrayList<>();
        // Sorted for deterministic pairing and output.
        List<Map.Entry<String, Set<String>>> entries = new ArrayList<>(new TreeMap<>(keysets).entrySet());

        for (int i = 0; i < entries.size(); i++) {
            String a = entries.get(i).getKey();
            Set<String> ksA = entries.get(i).getValue();
            for (int j = i + 1; j < entries.size(); j++) {
                String b = entries.get(j).getKey();
                Set<String> ksB = entries.get(j).getValue();

                if (!ksA.equals(ksB)) {
                    continue; // ED matches the exact chord; only identical chords clash
                }
                if (BindingConflictRules.isSafeOverlap(a, b)) {
                    continue; // different vehicle state or a sub-mode overlay → never co-fire
                }
                conflicts.add(new Conflict(a, b, BindingConflictRules.describe(a, b)));
            }
        }
        return conflicts;
    }

    /**
     * Reports the binding (if any) whose chord is identical to the candidate ({@code key} +
     * {@code modifiers}) for {@code bindingId} within the same context, or {@code null} if the
     * candidate is free. Used by the editor save-guard and the live keyboard widget. The binding's
     * own other slot is never treated as a self-conflict.
     */
    public static CandidateConflict candidateConflict(
            String bindingId, String key, Collection<String> modifiers,
            Map<String, KeyBindingsParser.KeyBinding> existingBindings) {
        return candidateConflict(bindingId, buildKeyset(key, modifiers), toKeysets(existingBindings));
    }

    /**
     * Keyset-based core, so it can be tested without KeyBindings.
     */
    static CandidateConflict candidateConflict(String bindingId, Set<String> candidate, Map<String, Set<String>> existing) {
        if (candidate.isEmpty() || existing == null) {
            return null;
        }
        for (Map.Entry<String, Set<String>> e : existing.entrySet()) {
            if (e.getKey().equals(bindingId)) {
                continue; // a binding never conflicts with its own other slot
            }
            if (!candidate.equals(e.getValue())) {
                continue; // exact chord match only
            }
            if (BindingConflictRules.isSafeOverlap(bindingId, e.getKey())) {
                continue;
            }
            return new CandidateConflict(e.getKey());
        }
        return null;
    }

    private static Set<String> buildKeyset(String key, Collection<String> modifiers) {
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
     * The full set of keys a binding's chord requires held: its main key plus all modifiers.
     */
    static Set<String> keysetOf(KeyBindingsParser.KeyBinding kb) {
        if (kb == null) {
            return Set.of();
        }
        return buildKeyset(kb.key, kb.modifiers == null ? null : Arrays.asList(kb.modifiers));
    }

    private static Map<String, Set<String>> toKeysets(Map<String, KeyBindingsParser.KeyBinding> bindings) {
        Map<String, Set<String>> keysets = new LinkedHashMap<>();
        if (bindings != null) {
            for (Map.Entry<String, KeyBindingsParser.KeyBinding> e : bindings.entrySet()) {
                Set<String> ks = keysetOf(e.getValue());
                if (!ks.isEmpty()) {
                    keysets.put(e.getKey(), ks);
                }
            }
        }
        return keysets;
    }
}
