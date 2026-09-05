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
    void orderedChordsPutCombosBeforePlainKeys() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();

        // OS-reserved chords (e.g. Alt+F4) are filtered out of the pool, so each count is the full
        // product minus those exclusions - computed the same way the pool builds it.
        long combos = comboCount(SafeKeyboardKeys.baseKeys());
        long plain = plainCount(SafeKeyboardKeys.baseKeys());
        assertEquals(combos + plain, chords.size());

        // First chord is a combo on the first base key with the first safe modifier (not reserved).
        assertEquals(SafeKeyboardKeys.baseKeys().get(0), chords.get(0).key());
        assertEquals(SafeKeyboardKeys.safeModifiers().get(0), chords.get(0).modifier());

        assertTrue(chords.subList(0, (int) combos).stream().allMatch(SafeKeyboardKeys.Chord::hasModifier));
        assertTrue(chords.subList((int) combos, chords.size()).stream()
                .noneMatch(SafeKeyboardKeys.Chord::hasModifier));
    }

    @Test
    void theNumpadIsNeverInTheAutoAssignPool() {
        // Rule: never auto-fill with a numpad key - this commander's keyboard may not have one.
        // The keys stay fully supported for manual binding and for execution, just not here.
        for (String key : SafeKeyboardKeys.NUMPAD_KEYS_NEVER_AUTO_ASSIGNED) {
            assertFalse(SafeKeyboardKeys.baseKeys().contains(key), "numpad key in the auto-assign pool: " + key);
            assertTrue(EliteKeyboardKeys.isAssignable(key), "numpad key must stay manually assignable: " + key);
        }
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords()) {
            assertFalse(chord.key().startsWith("Key_Numpad"), "numpad chord offered by the pool: " + chord.key());
        }
    }

    @Test
    void everyChordHasARealMainKeyAndNoBlankModifier() {
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords()) {
            assertNotNull(chord.key());
            assertFalse(chord.key().isBlank(), "chord with a blank main key");
            assertFalse(BindingModifier.isSupportedKeyboardModifier("Keyboard", chord.key()),
                    "modifier offered as a main key: " + chord.key());
            if (chord.hasModifier()) {
                assertTrue(chord.modifier().isSupportedKeyboardModifier(),
                        "blank or unsupported modifier in the pool: " + chord.modifier());
            }
        }
    }

    private static long comboCount(List<String> keys) {
        long combos = 0;
        for (String key : keys) {
            for (BindingModifier modifier : SafeKeyboardKeys.safeModifiers()) {
                if (!ReservedKeyChords.isOsReserved(key, List.of(modifier.key()))) {
                    combos++;
                }
            }
        }
        return combos;
    }

    private static long plainCount(List<String> keys) {
        return keys.stream().filter(key -> !ReservedKeyChords.isOsReserved(key, List.of())).count();
    }

    @Test
    void chordsAreUnique() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();
        assertEquals(chords.size(), Set.copyOf(chords).size(), "ordered chords must be distinct");
    }
}
