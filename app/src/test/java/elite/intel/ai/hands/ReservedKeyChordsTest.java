package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
    void theGameMenuKeyIsReservedWithAnyModifiersAndWithNone() {
        // Pause on Key_P: Elite opens the menu on P however it is modified, so the whole key goes.
        Set<String> menuKeys = Set.of("Key_P");
        assertTrue(ReservedKeyChords.isReserved("Key_P", List.of(), menuKeys));
        assertTrue(ReservedKeyChords.isReserved("Key_P", List.of("Key_LeftAlt"), menuKeys));
        assertTrue(ReservedKeyChords.isReserved("Key_P", List.of("Key_LeftShift"), menuKeys));
        assertTrue(ReservedKeyChords.isReserved("Key_P", List.of("Key_LeftControl", "Key_RightShift"), menuKeys));

        // P is taboo only because this commander put the menu there - it is not a property of the key.
        assertFalse(ReservedKeyChords.isReserved("Key_P", List.of("Key_LeftAlt"), Set.of()));
        assertFalse(ReservedKeyChords.isReserved("Key_P", List.of(), Set.of()));
        assertFalse(ReservedKeyChords.isOsReserved("Key_P", List.of("Key_LeftAlt")));

        // Another commander, another key: whatever sits on the game menu is the reserved one.
        assertTrue(ReservedKeyChords.isReserved("Key_G", List.of("Key_LeftShift"), Set.of("Key_G")));
        assertFalse(ReservedKeyChords.isReserved("Key_P", List.of(), Set.of("Key_G")));
    }

    @Test
    void theGameMenuKeyIsReservedOnlyWhereItIsTheMainKey() {
        // A key held as a modifier alongside something else does not open the menu, so reserving it
        // there would take a key off the board for nothing.
        assertFalse(ReservedKeyChords.isReserved("Key_G", List.of("Key_LeftShift"), Set.of("Key_LeftShift")));
    }

    @Test
    void gameMenuKeysComeOutOfTheCommandersFile() {
        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeys(bindings), "an unbound game menu reserves nothing");
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeys(null));

        bindings.put("Pause", binding("Key_P"));
        assertEquals(Set.of("Key_P"), ReservedKeyChords.gameMenuKeys(bindings));

        bindings.put("Pause", binding("Key_"));  // the empty-slot placeholder
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeys(bindings));
    }

    @Test
    void ordinaryChordsAreNotReserved() {
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_F4"), true));               // F4 alone
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_F1", "Key_LeftAlt"), true)); // Alt+F1 (not F4, not Ctrl+Alt)
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_J", "Key_LeftControl"), true));
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_G"), true));
        // Alt+P is not a rule of its own: it was only ever dangerous on files with Pause on Key_P.
        assertFalse(ReservedKeyChords.isReservedKeyset(Set.of("Key_P", "Key_LeftAlt"), true));
    }

    @Test
    void publicApiBuildsKeysetFromMainKeyAndModifiers() {
        assertTrue(ReservedKeyChords.isOsReserved("Key_F4", List.of("Key_LeftAlt")));
        assertFalse(ReservedKeyChords.isOsReserved("Key_F4", List.of()));
        assertFalse(ReservedKeyChords.isOsReserved("Key_G", List.of("Key_LeftShift")));
        assertTrue(ReservedKeyChords.isReserved("Key_F4", List.of("Key_LeftAlt"), Set.of()));
    }

    @Test
    void scanReportsControlsAlreadyBoundToAReservedChord() {
        // Elite's own controls screen has no reserved-chord rule, so a file written there can hold one.
        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        bindings.put("Pause", binding("Key_P"));
        bindings.put("QuickCommsPanel", binding("Key_P", "Key_LeftAlt"));
        bindings.put("GalaxyMapOpen", binding("Key_F4", "Key_RightAlt"));
        bindings.put("LandingGearToggle", binding("Key_L"));                  // clean
        bindings.put("ToggleCargoScoop", binding("Key_R", "Key_LeftShift"));  // clean

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(bindings);

        // Action-name order, so the spoken list and the log lines agree on every start. The game-menu
        // control itself is not in the list - its key is reserved for it.
        assertEquals(List.of("GalaxyMapOpen", "QuickCommsPanel"),
                found.stream().map(ReservedKeyChords.ReservedBinding::action).toList());
        assertEquals(Set.of("Key_P", "Key_LeftAlt"), found.get(1).chord());
        assertTrue(found.get(1).reason().contains("game menu"));
    }

    @Test
    void scanReportsEveryControlSharingTheGameMenuKeyWhateverItsModifiers() {
        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        bindings.put("Pause", binding("Key_G"));
        bindings.put("LandingGearToggle", binding("Key_G"));                  // the bare key
        bindings.put("ToggleCargoScoop", binding("Key_G", "Key_LeftShift"));  // and any chord on it
        bindings.put("GalaxyMapOpen", binding("Key_G", "Key_LeftControl", "Key_LeftAlt"));
        bindings.put("HyperSuperCombination", binding("Key_H"));              // clean

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(bindings);

        assertEquals(List.of("GalaxyMapOpen", "LandingGearToggle", "ToggleCargoScoop"),
                found.stream().map(ReservedKeyChords.ReservedBinding::action).toList());
    }

    @Test
    void anUnboundGameMenuReservesNothing() {
        // The recommended state: Esc opens that menu anyway, so no key is spent on it.
        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        bindings.put("LandingGearToggle", binding("Key_L"));
        bindings.put("ToggleCargoScoop", binding("Key_P", "Key_LeftAlt"));
        assertTrue(ReservedKeyChords.scan(bindings).isEmpty());
    }

    @Test
    void reasonNamesTheActualConsequencePerRule() {
        // Not interchangeable: one quits the game, one leaves the desktop session, one pauses the game.
        assertTrue(ReservedKeyChords.reason("Key_F4", List.of("Key_LeftAlt"), Set.of())
                .contains("closes the game"));
        assertTrue(ReservedKeyChords.reason("Key_F1", List.of("Key_LeftControl", "Key_LeftAlt"), Set.of(), true)
                .contains("virtual terminal"));
        assertTrue(ReservedKeyChords.reason("Key_P", List.of("Key_RightAlt"), Set.of("Key_P"))
                .contains("game menu"));
        assertNull(ReservedKeyChords.reason("Key_L", List.of(), Set.of("Key_P")));
    }

    @Test
    void scanIgnoresUnboundAndEmptyInput() {
        assertTrue(ReservedKeyChords.scan(null).isEmpty());
        assertTrue(ReservedKeyChords.scan(Map.of()).isEmpty());

        Map<String, KeyBindingsParser.KeyBinding> bindings = new LinkedHashMap<>();
        bindings.put("QuickCommsPanel", binding("Key_"));   // the empty-slot placeholder
        bindings.put("GalaxyMapOpen", binding(null, "Key_LeftAlt"));
        assertTrue(ReservedKeyChords.scan(bindings).isEmpty());
    }

    private static KeyBindingsParser.KeyBinding binding(String key, String... modifiers) {
        return KeyBindingsParser.getInstance().new KeyBinding(key, modifiers, false);
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
