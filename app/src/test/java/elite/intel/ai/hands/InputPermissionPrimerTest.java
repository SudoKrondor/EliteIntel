package elite.intel.ai.hands;

import elite.intel.bindforge.io.KeyBindingsParser;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputPermissionPrimerTest {

    private static final KeyBindingsParser PARSER = KeyBindingsParser.getInstance();

    private static KeyBindingsParser.BindingSlots primary(String key, String... modifiers) {
        return new KeyBindingsParser.BindingSlots(PARSER.new KeyBinding(key, modifiers, false), null);
    }

    @Test
    void leftControlWhenNoBindsFileWasRead() {
        assertEquals("Key_LeftControl", InputPermissionPrimer.freeKey(null).orElseThrow().frontierKey());
    }

    @Test
    void leftControlUsedOnlyAsAModifierIsStillFree() {
        Map<String, KeyBindingsParser.BindingSlots> slots = Map.of("LandingGearToggle", primary("Key_3", "Key_LeftControl"));
        assertEquals("Key_LeftControl", InputPermissionPrimer.freeKey(slots).orElseThrow().frontierKey());
    }

    @Test
    void rightControlWhenLeftControlIsBoundBare() {
        Map<String, KeyBindingsParser.BindingSlots> slots = Map.of("UseBoostJuice", primary("Key_LeftControl"));
        assertEquals("Key_RightControl", InputPermissionPrimer.freeKey(slots).orElseThrow().frontierKey());
    }

    @Test
    void noKeyWhenBothControlsAreBoundBare() {
        Map<String, KeyBindingsParser.BindingSlots> slots = Map.of(
                "UseBoostJuice", primary("Key_LeftControl"),
                "FireGroupNext", new KeyBindingsParser.BindingSlots(null, PARSER.new KeyBinding("Key_RightControl", new String[0], false)));
        assertEquals(Optional.empty(), InputPermissionPrimer.freeKey(slots));
    }
}
