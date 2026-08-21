package elite.intel.ai.hands;

import elite.intel.ai.hands.KeyBindingExecutor.NormalizedChord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link KeyBindingExecutor#normalizeChord} re-classifies a chord by key
 * identity rather than by Frontier's positional {@code <Primary>}/{@code <Modifier>} slots,
 * so that the actual action key is always the one tapped last while real Ctrl/Shift/Alt
 * modifiers are held.
 */
class KeyBindingExecutorTest {

    @Test
    void numpadKeysTheCommanderBoundThemselvesStillExecute() {
        // Auto-assignment never picks the numpad (this commander's keyboard may not have one),
        // but that is an assignment policy only: a numpad binding the commander made by hand
        // must still be pressable.
        for (String key : SafeKeyboardKeys.NUMPAD_KEYS_NEVER_AUTO_ASSIGNED) {
            assertNotNull(KeyBindingExecutor.resolveKeyCode(key), "numpad key not executable: " + key);
        }
        assertNotNull(KeyBindingExecutor.resolveKeyCode("Key_Numpad_Enter"));
    }

    @Test
    void fdevBindingWithActionKeyParkedInModifierSlotTapsTheActionKey() {
        // Exactly the buggy Frontier binding: a modifier (LeftControl) sits in <Primary>,
        // and the real action key (Key_Y) is parked in a <Modifier> slot.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_LeftControl",
                new String[]{"Key_LeftShift", "Key_LeftAlt", "Key_Y"});

        assertEquals(NormalizedChord.Status.OK, chord.status());
        // Y is the trigger and must be tapped last, NOT held for the whole chord.
        assertEquals("Key_Y", chord.triggerKey());
        // All three real modifiers are held; the labelled-primary modifier is included.
        assertEquals(
                java.util.List.of("Key_LeftControl", "Key_LeftShift", "Key_LeftAlt"),
                chord.modifierKeys());
    }

    @Test
    void conventionalChordIsUnchanged() {
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_Y",
                new String[]{"Key_LeftShift", "Key_LeftAlt"});

        assertEquals(NormalizedChord.Status.OK, chord.status());
        assertEquals("Key_Y", chord.triggerKey());
        assertEquals(java.util.List.of("Key_LeftShift", "Key_LeftAlt"), chord.modifierKeys());
    }

    @Test
    void plainKeyWithoutModifiers() {
        NormalizedChord chord = KeyBindingExecutor.normalizeChord("Key_Y", new String[]{});

        assertEquals(NormalizedChord.Status.OK, chord.status());
        assertEquals("Key_Y", chord.triggerKey());
        assertTrue(chord.modifierKeys().isEmpty());
    }

    @Test
    void allModifierChordTapsTheLabelledPrimary() {
        // Every key is a modifier, which Elite still accepts: the chord fires when the primary
        // goes down with the rest already held. Identity re-classification has nothing to sort,
        // so the file's own <Primary> label decides the trigger.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_RightControl",
                new String[]{"Key_LeftControl", "Key_LeftShift", "Key_LeftAlt"});

        assertEquals(NormalizedChord.Status.MODIFIER_ONLY, chord.status());
        assertEquals("Key_RightControl", chord.triggerKey());
        assertEquals(java.util.List.of("Key_LeftControl", "Key_LeftShift", "Key_LeftAlt"), chord.modifierKeys());
    }

    @Test
    void commanderBoundUiBackToControlPlusAlt() {
        // Reported from the field: UI_Back on Ctrl+Alt was skipped entirely, which left the app
        // unable to close any panel or back out of any menu (UI_Back backs BINDING_EXIT_KEY).
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_LeftAlt",
                new String[]{"Key_LeftControl"});

        assertEquals(NormalizedChord.Status.MODIFIER_ONLY, chord.status());
        assertEquals("Key_LeftAlt", chord.triggerKey());
        assertEquals(java.util.List.of("Key_LeftControl"), chord.modifierKeys());
    }

    @Test
    void chordWithNoPrimaryKeyHasNothingToPress() {
        // The only genuinely unexecutable shape left: nothing in the primary slot, so no key
        // supplies the trigger edge. KeyBindingsParser refuses to build such a binding, so this
        // guards the defensive branch rather than a reachable file.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                null,
                new String[]{"Key_LeftControl", "Key_LeftShift"});

        assertEquals(NormalizedChord.Status.NO_TRIGGER, chord.status());
        assertNull(chord.triggerKey());
    }

    @Test
    void ambiguousChordPrefersLabelledPrimaryAsTrigger() {
        // Two action keys (Y and T). Y sits in <Primary>, so it stays the trigger; T is held.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_Y",
                new String[]{"Key_T", "Key_LeftShift"});

        assertEquals(NormalizedChord.Status.AMBIGUOUS, chord.status());
        assertEquals("Key_Y", chord.triggerKey());
        assertEquals(java.util.List.of("Key_T", "Key_LeftShift"), chord.modifierKeys());
    }

    @Test
    void ambiguousChordWithModifierPrimaryFallsBackToFirstActionKey() {
        // A modifier sits in <Primary> and two action keys (Y, T) are in <Modifier> slots.
        // The labelled primary cannot be the trigger, so the first action key is tapped.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_LeftControl",
                new String[]{"Key_Y", "Key_T"});

        assertEquals(NormalizedChord.Status.AMBIGUOUS, chord.status());
        assertEquals("Key_Y", chord.triggerKey());
        assertEquals(java.util.List.of("Key_LeftControl", "Key_T"), chord.modifierKeys());
    }
}
