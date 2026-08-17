package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SafeKeyboardKeysTest {

    @Test
    void baseKeysIncludeRelaxedLayoutKeysAndAreAllAssignable() {
        // AZERTY safety guard relaxed 2026-06-24 (testers confirmed these work). This locks the
        // relaxation; restore the strict guard by dropping LAYOUT_VARIABLE_KEYS in SafeKeyboardKeys.
        List<String> base = SafeKeyboardKeys.baseKeys();
        for (String relaxed : List.of("Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y")) {
            assertTrue(base.contains(relaxed), "expected relaxed key in pool: " + relaxed);
        }
        for (String key : base) {
            assertTrue(EliteKeyboardKeys.isAssignable(key), "pool key not assignable: " + key);
        }
    }

    @Test
    void everyBaseKeyIsAssignableByTheWriter() {
        for (String key : SafeKeyboardKeys.baseKeys()) {
            assertTrue(EliteKeyboardKeys.isAssignable(key), "not assignable: " + key);
        }
    }

    @Test
    void safeModifiersExcludeRightAltAndAreAllSupported() {
        for (BindingModifier modifier : SafeKeyboardKeys.safeModifiers()) {
            assertTrue(modifier.isSupportedKeyboardModifier(), "unsupported modifier: " + modifier);
            assertFalse("Key_RightAlt".equals(modifier.key()), "RightAlt (AltGr) must not be a safe modifier");
        }
    }

    @Test
    void orderedChordsPutCombosBeforePlainKeysWithinEachTier() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();

        // OS-reserved chords (e.g. Alt+F4) are filtered out of the pool, so each count is the full
        // product minus those exclusions - computed the same way the pool builds it.
        long preferredCombos = comboCount(SafeKeyboardKeys.preferredKeys());
        long preferredPlain = plainCount(SafeKeyboardKeys.preferredKeys());
        long deferredCombos = comboCount(SafeKeyboardKeys.deferredKeys());
        long deferredPlain = plainCount(SafeKeyboardKeys.deferredKeys());
        assertEquals(preferredCombos + preferredPlain + deferredCombos + deferredPlain, chords.size());

        // First chord is a combo on the first preferred key with the first safe modifier (not reserved).
        assertEquals(SafeKeyboardKeys.preferredKeys().get(0), chords.get(0).key());
        assertEquals(SafeKeyboardKeys.safeModifiers().get(0), chords.get(0).modifier());

        // Tier 1: every preferred combo, then every plain preferred key.
        assertTrue(chords.subList(0, (int) preferredCombos).stream().allMatch(SafeKeyboardKeys.Chord::hasModifier));
        int firstPlain = (int) preferredCombos;
        assertTrue(chords.subList(firstPlain, firstPlain + (int) preferredPlain).stream()
                .noneMatch(SafeKeyboardKeys.Chord::hasModifier));
    }

    @Test
    void numpadChordsComeAfterEveryPreferredChord() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();
        int firstNumpad = -1;
        int lastPreferred = -1;
        for (int i = 0; i < chords.size(); i++) {
            boolean numpad = chords.get(i).key().startsWith("Key_Numpad_");
            if (numpad && firstNumpad < 0) {
                firstNumpad = i;
            }
            if (!numpad) {
                lastPreferred = i;
            }
        }
        assertTrue(firstNumpad > 0, "expected numpad chords in the pool as a last resort");
        assertTrue(firstNumpad > lastPreferred,
                "every non-numpad chord must be handed out before the first numpad chord");
    }

    @Test
    void numpadKeysStayInThePoolButOnlyInTheDeferredTier() {
        // The numpad is layout-stable and perfectly assignable - a commander who has one can
        // still use it. It is only deprioritised for auto-assignment.
        List<String> numpad = SafeKeyboardKeys.baseKeys().stream()
                .filter(key -> key.startsWith("Key_Numpad_"))
                .toList();
        assertEquals(15, numpad.size(), "every numpad token must remain assignable");
        assertEquals(numpad, SafeKeyboardKeys.deferredKeys());
        for (String key : SafeKeyboardKeys.preferredKeys()) {
            assertFalse(key.startsWith("Key_Numpad_"), "numpad key must not be in the preferred tier: " + key);
        }
    }

    @Test
    void preferredAndDeferredTiersPartitionTheBaseKeys() {
        List<String> rejoined = new java.util.ArrayList<>(SafeKeyboardKeys.preferredKeys());
        rejoined.addAll(SafeKeyboardKeys.deferredKeys());
        assertEquals(SafeKeyboardKeys.baseKeys().size(), rejoined.size(), "tiers must not drop a key");
        assertEquals(Set.copyOf(SafeKeyboardKeys.baseKeys()), Set.copyOf(rejoined));
    }

    private static long comboCount(List<String> keys) {
        long combos = 0;
        for (String key : keys) {
            for (BindingModifier modifier : SafeKeyboardKeys.safeModifiers()) {
                if (!ReservedKeyChords.isReserved(key, List.of(modifier.key()))) {
                    combos++;
                }
            }
        }
        return combos;
    }

    private static long plainCount(List<String> keys) {
        return keys.stream().filter(key -> !ReservedKeyChords.isReserved(key, List.of())).count();
    }

    @Test
    void chordsAreUnique() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();
        assertEquals(chords.size(), Set.copyOf(chords).size(), "ordered chords must be distinct");
    }
}
