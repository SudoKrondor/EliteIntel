package elite.intel.ui.widget;

import elite.intel.ai.hands.BindingModifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyChordCaptureFieldTest {

    @Test
    void chordOfPreservesModifierOrderAndTagsThemAsKeyboard() {
        LinkedHashSet<String> held = new LinkedHashSet<>(List.of("Key_LeftControl", "Key_LeftShift"));

        KeyChordCaptureField.CapturedChord chord = KeyChordCaptureField.chordOf(held, "Key_N");

        assertEquals(List.of(
                new BindingModifier("Keyboard", "Key_LeftControl"),
                new BindingModifier("Keyboard", "Key_LeftShift")
        ), chord.modifiers());
        assertEquals("Key_N", chord.key());
    }

    @Test
    void chordOfWithNoModifiersIsAPlainKey() {
        KeyChordCaptureField.CapturedChord chord = KeyChordCaptureField.chordOf(new LinkedHashSet<>(), "Key_M");

        assertEquals(List.of(), chord.modifiers());
        assertEquals("Key_M", chord.key());
    }
}
