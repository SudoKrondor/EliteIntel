package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<String, KeyBindingsParser.BindingSlots> slots = new LinkedHashMap<>();
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeysFromExecutableSlots(slots),
                "an unbound game menu reserves nothing");
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeysFromExecutableSlots(null));

        slots.put("Pause", primary("Key_P"));
        assertEquals(Set.of("Key_P"), ReservedKeyChords.gameMenuKeysFromExecutableSlots(slots));
    }

    @Test
    void bothSlotsOfTheGameMenuReserveTheirKeys() {
        // The case this method exists for: a commander with the menu on P in the Primary and O in the
        // Secondary has two keys that open it. The execution view keeps one slot per control, which is
        // right for pressing a key and wrong for deciding which keys are spent - offering O as free
        // hands out a key that pauses the game.
        Map<String, KeyBindingsParser.BindingSlots> slots = new LinkedHashMap<>();
        slots.put("Pause", new KeyBindingsParser.BindingSlots(binding("Key_P"), binding("Key_O")));

        assertEquals(Set.of("Key_P", "Key_O"), ReservedKeyChords.gameMenuKeysFromExecutableSlots(slots));
    }

    @Test
    void anUnboundOrAbsentGameMenuReservesNothingFromExecutableSlots() {
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeysFromExecutableSlots(Map.of()));
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeysFromExecutableSlots(
                Map.of("Pause", new KeyBindingsParser.BindingSlots(null, null))));
        // "Key_" is the empty-slot placeholder Elite writes, not a key.
        assertEquals(Set.of(), ReservedKeyChords.gameMenuKeysFromExecutableSlots(
                Map.of("Pause", new KeyBindingsParser.BindingSlots(binding("Key_"), null))));
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
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("Pause", primary("Key_P"));
        bindings.put("QuickCommsPanel", primary("Key_P", "Key_LeftAlt"));
        bindings.put("GalaxyMapOpen", primary("Key_F4", "Key_RightAlt"));
        bindings.put("LandingGearToggle", primary("Key_L"));                  // clean
        bindings.put("ToggleCargoScoop", primary("Key_R", "Key_LeftShift"));  // clean

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
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("Pause", primary("Key_G"));
        bindings.put("LandingGearToggle", primary("Key_G"));                  // the bare key
        bindings.put("ToggleCargoScoop", primary("Key_G", "Key_LeftShift"));  // and any chord on it
        bindings.put("GalaxyMapOpen", primary("Key_G", "Key_LeftControl", "Key_LeftAlt"));
        bindings.put("HyperSuperCombination", primary("Key_H"));              // clean

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(bindings);

        assertEquals(List.of("GalaxyMapOpen", "LandingGearToggle", "ToggleCargoScoop"),
                found.stream().map(ReservedKeyChords.ReservedBinding::action).toList());
    }

    @Test
    void anUnboundGameMenuReservesNothing() {
        // The recommended state: Esc opens that menu anyway, so no key is spent on it.
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("LandingGearToggle", primary("Key_L"));
        bindings.put("ToggleCargoScoop", primary("Key_P", "Key_LeftAlt"));
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

        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("QuickCommsPanel", primary("Key_"));   // the empty-slot placeholder
        bindings.put("GalaxyMapOpen", primary(null, "Key_LeftAlt"));
        assertTrue(ReservedKeyChords.scan(bindings).isEmpty());
    }

    private static KeyBindingsParser.KeyBinding binding(String key, String... modifiers) {
        return KeyBindingsParser.getInstance().new KeyBinding(key, modifiers, false);
    }

    /**
     * One chord in the Primary slot and nothing in the Secondary.
     */
    private static KeyBindingsParser.BindingSlots primary(String key, String... modifiers) {
        return new KeyBindingsParser.BindingSlots(binding(key, modifiers), null);
    }

    @Test
    void scanReportsAReservedChordSittingInASecondarySlot() {
        // Elite fires either slot, so Alt+F4 in a Secondary closes the game exactly as it would in a
        // Primary, and a Secondary on the menu key pauses it. A scan of the one slot EliteIntel presses
        // called both of these clean.
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("Pause", primary("Key_P"));
        bindings.put("GalaxyMapOpen", new KeyBindingsParser.BindingSlots(
                binding("Key_M"), binding("Key_F4", "Key_LeftAlt")));
        bindings.put("LandingGearToggle", new KeyBindingsParser.BindingSlots(
                binding("Key_L"), binding("Key_P", "Key_LeftShift")));

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(bindings);

        assertEquals(List.of("GalaxyMapOpen", "LandingGearToggle"),
                found.stream().map(ReservedKeyChords.ReservedBinding::action).toList());
        assertEquals(Set.of("Key_F4", "Key_LeftAlt"), found.get(0).chord());
        assertEquals(ReservedKeyChords.Rule.OS_CLAIMED, found.get(0).rule());
        assertEquals(Set.of("Key_P", "Key_LeftShift"), found.get(1).chord());
        assertEquals(ReservedKeyChords.Rule.GAME_MENU, found.get(1).rule());
    }

    @Test
    void aControlReservedOnBothSlotsIsReportedOncePerSlot() {
        // Two chords to move, so two lines - Primary first, so the log reads in slot order.
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("Pause", primary("Key_P"));
        bindings.put("EjectAllCargo", new KeyBindingsParser.BindingSlots(
                binding("Key_P", "Key_LeftControl"), binding("Key_P", "Key_LeftAlt")));

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(bindings);

        assertEquals(2, found.size());
        assertEquals(Set.of("Key_P", "Key_LeftControl"), found.get(0).chord());
        assertEquals(Set.of("Key_P", "Key_LeftAlt"), found.get(1).chord());
    }

    @Test
    void autoFixPoolNeverOffersAltF4() {
        boolean hasAltF4 = SafeKeyboardKeys.orderedChords().stream().anyMatch(c ->
                "Key_F4".equals(c.key())
                        && c.modifier() != null
                        && ("Key_LeftAlt".equals(c.modifier().key()) || "Key_RightAlt".equals(c.modifier().key())));
        assertFalse(hasAltF4, "Auto-fix pool must not contain Alt+F4");
    }

    @Test
    void eachFoundBindingCarriesTheRuleThatDecidesTheFix() {
        Map<String, KeyBindingsParser.BindingSlots> bindings = new LinkedHashMap<>();
        bindings.put("Pause", primary("Key_P"));
        bindings.put("EjectAllCargo", primary("Key_P", "Key_LeftControl"));
        bindings.put("CloseWindow", primary("Key_F4", "Key_LeftAlt"));

        Map<String, ReservedKeyChords.Rule> byAction = ReservedKeyChords.scan(bindings).stream()
                .collect(Collectors.toMap(
                        ReservedKeyChords.ReservedBinding::action, ReservedKeyChords.ReservedBinding::rule));

        // The two are fixed differently: one clears the game menu, the other moves the control.
        assertEquals(ReservedKeyChords.Rule.GAME_MENU, byAction.get("EjectAllCargo"));
        assertEquals(ReservedKeyChords.Rule.OS_CLAIMED, byAction.get("CloseWindow"));
        assertNotEquals(
                ReservedKeyChords.Rule.GAME_MENU.remedy(), ReservedKeyChords.Rule.OS_CLAIMED.remedy());
        for (ReservedKeyChords.Rule rule : ReservedKeyChords.Rule.values()) {
            assertFalse(rule.remedy().isBlank(), rule + " has no remedy for the log line");
        }
    }
}
