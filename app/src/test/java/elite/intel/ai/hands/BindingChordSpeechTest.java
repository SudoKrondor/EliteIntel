package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The spoken binding warning names the commander's own keys, so these cover the tokens a
 * {@code .binds} file actually carries - including the ones the Bindings-tab formatter renders in a
 * form no voice can read ({@code Key_UpArrow} as "UpArrow").
 */
class BindingChordSpeechTest {

    @Test
    void aBareLetterKeyIsSpokenAsTheLetter() {
        assertEquals("W", BindingChordSpeech.describe(Set.of("Key_W")));
    }

    @Test
    void compoundKeyNamesAreSplitIntoWords() {
        assertEquals("Up Arrow", BindingChordSpeech.describe(Set.of("Key_UpArrow")));
        assertEquals("Page Down", BindingChordSpeech.describe(Set.of("Key_PageDown")));
        assertEquals("Numpad 4", BindingChordSpeech.describe(Set.of("Key_Numpad_4")));
        assertEquals("F 5", BindingChordSpeech.describe(Set.of("Key_F5")));
    }

    @Test
    void modifiersAreSpokenFirstWhateverOrderTheKeySetArrivesIn() {
        // A chord is an unordered key-set - Elite's Primary/Modifier slots are positional only - so the
        // reading has to be imposed, or the same binding is announced differently from run to run.
        assertEquals("Left Control plus G", BindingChordSpeech.describe(Set.of("Key_G", "Key_LeftControl")));
        assertEquals("Left Alt plus Left Shift plus G",
                BindingChordSpeech.describe(Set.of("Key_LeftShift", "Key_G", "Key_LeftAlt")));
    }

    @Test
    void theFieldReportedWasdLayoutIsSpokenAsFourDistinctKeys() {
        // The four map/UI overlaps carry four chords; the commander needs the keys, not the repetition.
        List<String> spoken = BindingChordSpeech.distinctChords(List.of(
                Set.of("Key_W"), Set.of("Key_S"), Set.of("Key_A"), Set.of("Key_D")));

        assertEquals(List.of("A", "D", "S", "W"), spoken);
    }

    @Test
    void oneChordSharedBySeveralConflictsIsNamedOnce() {
        // The count is what picks singular or plural wording in the warning, so the duplicate has to be
        // gone before it is counted, not merely read out once.
        List<String> spoken = BindingChordSpeech.distinctChords(List.of(
                Set.of("Key_A"), Set.of("Key_A"), Set.of("Key_LeftArrow")));

        assertEquals(List.of("A", "Left Arrow"), spoken);
    }

    @Test
    void anEmptyChordContributesNothing() {
        assertEquals("", BindingChordSpeech.describe(Set.of()));
        assertEquals(List.of("A"), BindingChordSpeech.distinctChords(List.of(Set.of(), Set.of("Key_A"))));
    }
}
