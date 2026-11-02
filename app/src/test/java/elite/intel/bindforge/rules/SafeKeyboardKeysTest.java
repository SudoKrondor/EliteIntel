package elite.intel.bindforge.rules;

import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.EliteKeyboardKeys;
import elite.intel.bindforge.model.GameKeyboardLayout;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SafeKeyboardKeysTest {

    @Test
    void nonAzertyBaseKeysIncludeRelaxedLayoutKeysAndAreAllAssignable() {
        // Guard relaxed 2026-06-24 for non-AZERTY files (testers confirmed these work there).
        List<String> base = SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER);
        for (String relaxed : List.of("Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y")) {
            assertTrue(base.contains(relaxed), "expected relaxed key in pool: " + relaxed);
        }
        for (String key : base) {
            assertTrue(EliteKeyboardKeys.isAssignable(key), "pool key not assignable: " + key);
        }
    }

    @Test
    void theAzertyPoolIsStrict() {
        // A fr-FR file names the number row by its unshifted symbols, so Key_1..Key_9 and Key_Period are not keys
        // the game tells apart there: an auto-assigned Shift+Key_9 Hyperspace opened the Friends menu (Shift+Key_1).
        List<String> base = SafeKeyboardKeys.baseKeys(GameKeyboardLayout.AZERTY);
        for (String excluded : List.of("Key_0", "Key_1", "Key_9", "Key_Period", "Key_Comma", "Key_Slash",
                "Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y")) {
            assertFalse(base.contains(excluded), "AZERTY pool offers " + excluded);
        }
        assertTrue(base.contains("Key_E"));
        assertTrue(base.contains("Key_F1"));
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords(GameKeyboardLayout.AZERTY)) {
            assertTrue(GameKeyboardLayout.AZERTY.namesKey(chord.key()), "AZERTY cannot name " + chord.key());
            assertTrue(EliteKeyboardKeys.isAssignable(chord.key()), "not assignable: " + chord.key());
        }
    }

    @Test
    void everyBaseKeyIsAssignableByTheWriter() {
        for (String key : SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER)) {
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
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER);

        // OS-reserved chords (e.g. Alt+F4) are filtered out of the pool, so each count is the full
        // product minus those exclusions - computed the same way the pool builds it.
        long combos = comboCount(SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER));
        long plain = plainCount(SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER));
        assertEquals(combos + plain, chords.size());

        // First chord is a combo on the first base key with the first safe modifier (not reserved).
        assertEquals(SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER).get(0), chords.get(0).key());
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
            assertFalse(SafeKeyboardKeys.baseKeys(GameKeyboardLayout.OTHER).contains(key), "numpad key in the auto-assign pool: " + key);
            assertTrue(EliteKeyboardKeys.isAssignable(key), "numpad key must stay manually assignable: " + key);
        }
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER)) {
            assertFalse(chord.key().startsWith("Key_Numpad"), "numpad chord offered by the pool: " + chord.key());
        }
    }

    @Test
    void everyChordHasARealMainKeyAndNoBlankModifier() {
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER)) {
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
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER);
        assertEquals(chords.size(), Set.copyOf(chords).size(), "ordered chords must be distinct");
    }
}
