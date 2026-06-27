package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservedKeyChordsTest {

    @Test
    void altF4IsReservedOnEveryOs() {
        assertTrue(ReservedKeyChords.isReservedKeyset(Set.of("Key_F4", "Key_LeftAlt"), false));
        assertTrue(ReservedKeyChords.isReservedKeyset(Set.of("Key_F4", "Key_LeftAlt"), true));
        assertTrue(ReservedKeyChords.isReservedKeyset(Set.of("Key_F4", "Key_RightAlt"), false));
        // Order/extra modifiers do not let it slip through (key-set match).
        assertTrue(ReservedKeyChords.isReservedKeyset(
                Set.of("Key_LeftControl", "Key_LeftAlt", "Key_F4"), false));
    }

    @Test
    void ctrlAltFunctionKeysAreReservedOnlyOnLinux() {
        Set<String> ctrlAltF1 = Set.of("Key_F1", "Key_LeftControl", "Key_LeftAlt");
        assertTrue(ReservedKeyChords.isReservedKeyset(ctrlAltF1, true));
        assertFalse(ReservedKeyChords.isReservedKeyset(ctrlAltF1, false));

        // Whole F-row is covered on Linux.
        assertTrue(ReservedKeyChords.isReservedKeyset(Set.of("Key_F7", "Key_RightControl", "Key_RightAlt"), true));
    }

    @Test
    void ordinaryChordsAreNotReserved() {
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_F4"), true));               // F4 alone
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_F1", "Key_LeftAlt"), true)); // Alt+F1 (not F4, not Ctrl+Alt)
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_J", "Key_LeftControl"), true));
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_G"), true));
    }

    @Test
    void publicApiBuildsKeysetFromMainKeyAndModifiers() {
        assertTrue(ReservedKeyChords.isReserved("Key_F4", List.of("Key_LeftAlt")));
        assertFalse(ReservedKeyChords.isReserved("Key_F4", List.of()));
        assertFalse(ReservedKeyChords.isReserved("Key_G", List.of("Key_LeftShift")));
    }

    @Test
    void autoFixPoolNeverOffersAltF4() {
        boolean hasAltF4 = SafeKeyboardKeys.orderedChords().stream().anyMatch(c ->
                "Key_F4".equals(c.key())
                        && c.modifier() != null
                        && ("Key_LeftAlt".equals(c.modifier().key()) || "Key_RightAlt".equals(c.modifier().key())));
        assertFalse(hasAltF4, "Auto-fix pool must not contain Alt+F4");
    }
}
