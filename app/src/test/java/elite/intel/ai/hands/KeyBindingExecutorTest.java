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
    void allModifierChordHasNoTriggerEdge() {
        // Every key is a modifier: there is no non-modifier whose press forms a trigger edge.
        NormalizedChord chord = KeyBindingExecutor.normalizeChord(
                "Key_RightControl",
                new String[]{"Key_LeftControl", "Key_LeftShift", "Key_LeftAlt"});

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
