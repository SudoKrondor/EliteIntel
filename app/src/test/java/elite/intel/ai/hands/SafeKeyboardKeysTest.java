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

        int baseCount = SafeKeyboardKeys.baseKeys().size();
        int modifierCount = SafeKeyboardKeys.safeModifiers().size();
        assertEquals(baseCount * modifierCount + baseCount, chords.size());

        // First chord is a combo on the first base key with the first safe modifier.
        assertEquals(SafeKeyboardKeys.baseKeys().get(0), chords.get(0).key());
        assertEquals(SafeKeyboardKeys.safeModifiers().get(0), chords.get(0).modifier());

        // All combos come before any plain (unmodified) key.
        int firstPlain = -1;
        for (int i = 0; i < chords.size(); i++) {
            if (!chords.get(i).hasModifier()) {
                firstPlain = i;
                break;
            }
        }
        assertEquals(baseCount * modifierCount, firstPlain, "plain keys must follow every combo");
        for (int i = firstPlain; i < chords.size(); i++) {
            assertFalse(chords.get(i).hasModifier());
        }
    }

    @Test
    void chordsAreUnique() {
        List<SafeKeyboardKeys.Chord> chords = SafeKeyboardKeys.orderedChords();
        assertEquals(chords.size(), Set.copyOf(chords).size(), "ordered chords must be distinct");
    }
}
