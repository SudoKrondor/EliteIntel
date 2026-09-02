package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the rule that decides whether UI navigation survives a focused Elite text field: does the chord
 * put a character in the box, not is the chord modified.
 * <p>
 * The three layouts asserted end to end are the three that were actually observed, not invented ones -
 * Frontier's arrow default, the commander bundle of 2026-08-31 (bare W/A/S/D, arrows given to power
 * distribution, route plotting silently dead), and the Ctrl+W/A/S/D layout verified working in game the
 * same day. See {@link UiNavigationTextTrap}.
 */
class UiNavigationTextTrapTest {

    private static final KeyBindingsParser PARSER = KeyBindingsParser.getInstance();

    @Test
    void bareLetterTypesACharacter() {
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{}));
    }

    @Test
    void shiftKeepsTheCharacter() {
        // Shift changes the character rather than removing it, so the field still swallows it.
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{"Key_LeftShift"}));
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{"Key_RightShift"}));
    }

    @Test
    void controlAndAltSuppressTheCharacter() {
        assertFalse(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{"Key_LeftControl"}));
        assertFalse(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{"Key_LeftAlt"}));
        assertFalse(UiNavigationTextTrap.typesACharacter("Key_S", new String[]{"Key_LeftControl", "Key_LeftShift"}));
    }

    @Test
    void navigationKeysProduceNoCharacter() {
        for (String key : List.of("Key_DownArrow", "Key_UpArrow", "Key_LeftArrow", "Key_RightArrow",
                "Key_Tab", "Key_Enter", "Key_Home", "Key_End", "Key_PageUp", "Key_F5")) {
            assertFalse(UiNavigationTextTrap.typesACharacter(key, new String[]{}), key + " should be safe");
        }
    }

    @Test
    void spaceAndDigitsAndAccentedLettersType() {
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_Space", new String[]{}));
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_4", new String[]{}));
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_Comma", new String[]{}));
        assertTrue(UiNavigationTextTrap.typesACharacter("Key_é", new String[]{}));
    }

    @Test
    void unknownKeyTokenIsNotFlagged() {
        // Being wrong in the quiet direction is deliberate: this warning is spoken on every start.
        assertFalse(UiNavigationTextTrap.typesACharacter("Key_SomethingFrontierAddedLater", new String[]{}));
    }

    @Test
    void frontierArrowDefaultIsClean() {
        assertTrue(UiNavigationTextTrap.scan(layout(
                "Key_UpArrow", "Key_DownArrow", "Key_LeftArrow", "Key_RightArrow", new String[]{})).isEmpty());
    }

    @Test
    void controlWasdIsClean() {
        // Verified in game 2026-08-31: navigation works, because Ctrl emits no character.
        assertTrue(UiNavigationTextTrap.scan(layout(
                "Key_W", "Key_S", "Key_A", "Key_D", new String[]{"Key_LeftControl"})).isEmpty());
    }

    @Test
    void bareWasdFlagsAllFourDirections() {
        // The commander bundle of 2026-08-31.
        List<UiNavigationTextTrap.TrappedBinding> trapped = UiNavigationTextTrap.scan(layout(
                "Key_W", "Key_S", "Key_A", "Key_D", new String[]{}));

        assertEquals(4, trapped.size());
        assertEquals(List.of("UI_Up", "UI_Down", "UI_Left", "UI_Right"),
                trapped.stream().map(UiNavigationTextTrap.TrappedBinding::action).toList());
        assertEquals(java.util.Set.of("Key_S"),
                trapped.get(1).chord(), "the chord carries the keys the commander has to move");
    }

    @Test
    void shiftWasdFlagsAllFourDirections() {
        assertEquals(4, UiNavigationTextTrap.scan(layout(
                "Key_W", "Key_S", "Key_A", "Key_D", new String[]{"Key_LeftShift"})).size());
    }

    @Test
    void uiSelectIsDeliberatelyOutOfScope() {
        // Frontier's default UI_Select is bare Space, which types a character. Including Select would
        // flag nearly every commander alive, and it is only pressed once focus has left the field.
        Map<String, KeyBindingsParser.KeyBinding> bindings = layout(
                "Key_UpArrow", "Key_DownArrow", "Key_LeftArrow", "Key_RightArrow", new String[]{});
        bindings.put("UI_Select", PARSER.new KeyBinding("Key_Space", new String[]{}, false));

        assertTrue(UiNavigationTextTrap.scan(bindings).isEmpty());
    }

    @Test
    void unboundDirectionIsNotFlagged() {
        Map<String, KeyBindingsParser.KeyBinding> bindings = layout(
                "Key_W", "Key_S", "Key_A", "Key_D", new String[]{});
        bindings.remove("UI_Down");

        assertEquals(3, UiNavigationTextTrap.scan(bindings).size());
    }

    private static Map<String, KeyBindingsParser.KeyBinding> layout(String up, String down, String left,
                                                                    String right, String[] modifiers) {
        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        bindings.put("UI_Up", PARSER.new KeyBinding(up, modifiers, false));
        bindings.put("UI_Down", PARSER.new KeyBinding(down, modifiers, false));
        bindings.put("UI_Left", PARSER.new KeyBinding(left, modifiers, false));
        bindings.put("UI_Right", PARSER.new KeyBinding(right, modifiers, false));
        return bindings;
    }
}
