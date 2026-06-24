package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SafeKeyboardKeysTest {

    private static final Set<String> UNSAFE_KEYS = Set.of(
            "Key_Q", "Key_W", "Key_A", "Key_Z", "Key_M", "Key_Y");

    @Test
    void baseKeysNeverIncludeLayoutSensitiveKeysOrPunctuation() {
        for (String key : SafeKeyboardKeys.baseKeys()) {
            assertFalse(UNSAFE_KEYS.contains(key), "unsafe key in pool: " + key);
            // Only letters, digits, numpad, and function keys - never punctuation tokens.
            assertTrue(
                    key.matches("Key_[A-Z]|Key_[0-9]|Key_Numpad_.*|Key_F[0-9]{1,2}"),
                    "unexpected pool key: " + key);
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
