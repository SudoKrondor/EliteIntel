package elite.intel.ui.support;

import elite.intel.bindforge.io.KeyBindingsParser.BindingSlotType;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlot;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.storage.ConfigurationFiles;
import elite.intel.bindforge.storage.SavedDeviceLabels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A controller input reads in the grid as the name the user gave it in the Device Editor, as the game's own
 * control screen shows it - and as its number when it has none.
 */
class BindingSlotDisplayFormatterTest {

    private BindingSlotDisplayFormatter formatter;

    @BeforeEach
    void aStickWithOneNamedButton() throws IOException {
        ConfigurationFiles files = new ConfigurationFiles(Map.of(),
                "<Root><VKB-Stick><PID>0200</PID><VID>231D</VID></VKB-Stick></Root>".getBytes(StandardCharsets.UTF_8),
                Map.of("VKB-Stick.buttonMap", ("<Root><Joy_6>1st Hat Up</Joy_6><Joy_14>2nd Hat Left</Joy_14></Root>")
                        .getBytes(StandardCharsets.UTF_8)));
        formatter = new BindingSlotDisplayFormatter(SavedDeviceLabels.of(files));
    }

    @Test
    void aNamedInputReadsByItsName() {
        ReadOnlyBindingSlot slot = slot("VKB-Stick", "Joy_6", List.of());

        assertEquals("VKB-Stick | 1st Hat Up", formatter.formatSlot(slot));
        assertEquals("VKB-Stick Joy_6", formatter.rawTokens(slot));
    }

    @Test
    void anUnnamedInputReadsByItsNumberAndHidesNothing() {
        ReadOnlyBindingSlot slot = slot("VKB-Stick", "Joy_22", List.of());

        assertEquals("VKB-Stick | Joystick 22", formatter.formatSlot(slot));
        assertNull(formatter.rawTokens(slot));
    }

    @Test
    void aNamedModifierReadsByItsNameToo() {
        ReadOnlyBindingSlot slot = slot("Keyboard", "Key_G", List.of(new BindingModifier("VKB-Stick", "Joy_14")));

        assertEquals("Keyboard | VKB-Stick 2nd Hat Left + G", formatter.formatSlot(slot));
        assertEquals("VKB-Stick Joy_14 + Keyboard Key_G", formatter.rawTokens(slot));
    }

    @Test
    void withoutSavedNamesEveryInputReadsByNumber() {
        ReadOnlyBindingSlot slot = slot("VKB-Stick", "Joy_6", List.of());

        assertEquals("VKB-Stick | Joystick 6", new BindingSlotDisplayFormatter().formatSlot(slot));
    }

    @Test
    void aControllerSlotIsFoundByItsButtonNameDeviceAndToken() {
        String text = formatter.controllerSearchText(slot("VKB-Stick", "Joy_6", List.of()));

        assertTrue(text.contains("1st Hat Up"));
        assertTrue(text.contains("VKB-Stick"));
        assertTrue(text.contains("Joy_6"));
    }

    @Test
    void anUnnamedControllerSlotIsFoundByItsNumberAndToken() {
        String text = formatter.controllerSearchText(slot("VKB-Stick", "Joy_22", List.of()));

        assertTrue(text.contains("Joystick 22"));
        assertTrue(text.contains("Joy_22"));
    }

    @Test
    void aKeyboardOnlySlotAddsNothingToTheSearch() {
        assertEquals("", formatter.controllerSearchText(slot("Keyboard", "Key_Space", List.of())));
    }

    @Test
    void aKeyboardSlotWithAControllerModifierIsFoundByTheModifiersName() {
        ReadOnlyBindingSlot slot = slot("Keyboard", "Key_G", List.of(new BindingModifier("VKB-Stick", "Joy_14")));

        assertTrue(formatter.controllerSearchText(slot).contains("2nd Hat Left"));
    }

    private static ReadOnlyBindingSlot slot(String device, String key, List<BindingModifier> modifiers) {
        return new ReadOnlyBindingSlot(device, key, null, modifiers, false, BindingSlotType.PRIMARY,
                "Keyboard".equals(device), "Keyboard".equals(device));
    }
}
