package elite.intel.ui.widget;

import elite.intel.bindforge.model.BindingModifier;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void aDisabledFieldShowsItsChordAndCannotBeArmed() {
        KeyChordCaptureField field = new KeyChordCaptureField("Key_R", "Press a key", token -> token, chord -> fail());
        JButton surface = (JButton) field.getComponent(0);
        String idle = surface.getText();

        field.setEnabled(false);
        field.setToolTipText("Off");
        surface.doClick();

        assertFalse(surface.isEnabled());
        assertEquals(idle, surface.getText(), "a click armed a field that is off");
        assertEquals("Off", surface.getToolTipText());
    }

    @Test
    void disablingAnArmedFieldCancelsTheCapture() {
        KeyChordCaptureField field = new KeyChordCaptureField("Key_R", "Press a key", token -> token, chord -> fail());
        JButton surface = (JButton) field.getComponent(0);
        surface.doClick();
        assertNotEquals("Key_R", surface.getText(), "the click did not arm the field");

        field.setEnabled(false);

        assertEquals("Key_R", surface.getText());
    }
}
