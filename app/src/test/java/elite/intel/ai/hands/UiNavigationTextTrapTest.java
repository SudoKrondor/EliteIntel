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
        Map<String, KeyBindingsParser.BindingSlots> slots = layout(
                "Key_UpArrow", "Key_DownArrow", "Key_LeftArrow", "Key_RightArrow", new String[]{});
        slots.put("UI_Select", primaryOnly("Key_Space", new String[]{}));

        assertTrue(UiNavigationTextTrap.scan(slots).isEmpty());
    }

    @Test
    void unboundDirectionIsNotFlagged() {
        Map<String, KeyBindingsParser.BindingSlots> slots = layout(
                "Key_W", "Key_S", "Key_A", "Key_D", new String[]{});
        slots.remove("UI_Down");

        assertEquals(3, UiNavigationTextTrap.scan(slots).size());
    }

    @Test
    void aPrintableKeyInTheSecondarySlotIsTrappedToo() {
        // Arrows in the Primary, W/A/S/D in the Secondary: Elite fires either, so the S still lands in
        // the search box. A scan that read only the slot EliteIntel presses called this layout clean.
        Map<String, KeyBindingsParser.BindingSlots> slots = layout(
                "Key_UpArrow", "Key_DownArrow", "Key_LeftArrow", "Key_RightArrow", new String[]{});
        slots.put("UI_Down", new KeyBindingsParser.BindingSlots(
                PARSER.new KeyBinding("Key_DownArrow", new String[]{}, false),
                PARSER.new KeyBinding("Key_S", new String[]{}, false)));

        List<UiNavigationTextTrap.TrappedBinding> trapped = UiNavigationTextTrap.scan(slots);

        assertEquals(1, trapped.size());
        assertEquals("UI_Down", trapped.get(0).action());
        assertEquals(java.util.Set.of("Key_S"), trapped.get(0).chord(), "the Secondary's chord, not the arrow");
    }

    @Test
    void bothSlotsTrappedAreReportedOncePerSlot() {
        // Each chord is its own thing to move, so each is named - Primary first.
        Map<String, KeyBindingsParser.BindingSlots> slots = new LinkedHashMap<>();
        slots.put("UI_Up", new KeyBindingsParser.BindingSlots(
                PARSER.new KeyBinding("Key_W", new String[]{}, false),
                PARSER.new KeyBinding("Key_I", new String[]{}, false)));

        assertEquals(List.of(java.util.Set.of("Key_W"), java.util.Set.of("Key_I")),
                UiNavigationTextTrap.scan(slots).stream().map(UiNavigationTextTrap.TrappedBinding::chord).toList());
    }

    /**
     * The four directions with one chord each in the Primary slot and nothing in the Secondary, which
     * is how every layout observed so far was written.
     */
    private static Map<String, KeyBindingsParser.BindingSlots> layout(String up, String down, String left,
                                                                      String right, String[] modifiers) {
        Map<String, KeyBindingsParser.BindingSlots> slots = new LinkedHashMap<>();
        slots.put("UI_Up", primaryOnly(up, modifiers));
        slots.put("UI_Down", primaryOnly(down, modifiers));
        slots.put("UI_Left", primaryOnly(left, modifiers));
        slots.put("UI_Right", primaryOnly(right, modifiers));
        return slots;
    }

    private static KeyBindingsParser.BindingSlots primaryOnly(String key, String[] modifiers) {
        return new KeyBindingsParser.BindingSlots(PARSER.new KeyBinding(key, modifiers, false), null);
    }
}
