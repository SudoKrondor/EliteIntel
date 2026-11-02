package elite.intel.bindforge.rules;

import elite.intel.ai.hands.Bindings;
import elite.intel.bindforge.io.KeyBindingsParser;
import elite.intel.bindforge.io.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.bindforge.model.GameKeyboardLayout;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner.Plan;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner.PlannedEdit;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner.SkipReason;
import elite.intel.bindforge.rules.MissingBindingAutoAssigner.SkippedBinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MissingBindingAutoAssignerTest {

    @TempDir
    Path tempDir;

    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();
    private final MissingBindingAutoAssigner assigner = new MissingBindingAutoAssigner();

    @Test
    void emptyBindingGetsFirstSafeCombo() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(1, plan.edits().size());
        PlannedEdit edit = plan.edits().get(0);
        assertEquals("ToggleCargoScoop", edit.bindingId());
        assertEquals(BindingSlotType.PRIMARY, edit.slotType());
        // First chord in the pool: first base key + first safe modifier.
        assertEquals(SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER).get(0).key(), edit.key());
        assertEquals(SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER).get(0).modifier(), edit.modifier());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void controllerPrimaryFallsBackToEmptySecondary() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <UseBoostJuice>
                        <Primary Device="T16000MTHROTTLE" DeviceIndex="1" Key="Joy_POV1Right" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </UseBoostJuice>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(1, plan.edits().size());
        assertEquals(BindingSlotType.SECONDARY, plan.edits().get(0).slotType());
    }

    @Test
    void bothControllerSlotsAreSkipped() throws Exception {
        // Both slots hold a device the commander bound in the game. There is nothing to fill
        // and nothing may be taken away, whether or not EliteIntel drives this control - it is
        // reported and left exactly as the game wrote it.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <CycleFireGroupNext>
                        <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                        <Secondary Device="044F0404" DeviceIndex="0" Key="Joy_4" />
                    </CycleFireGroupNext>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertTrue(plan.edits().isEmpty(), "a controller assignment must never be planned away");
        assertEquals(1, plan.skipped().size());
        assertEquals(SkipReason.BOTH_SLOTS_OCCUPIED, plan.skipped().get(0).reason());
    }

    @Test
    void anExistingKeyboardBindingIsNeverReplaced() throws Exception {
        // Both slots are taken and one is a keyboard chord, so the control is already bound;
        // no plan may ever reach a key the commander chose.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <CycleFireGroupNext>
                        <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                        <Secondary Device="Keyboard" Key="Key_V" />
                    </CycleFireGroupNext>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertTrue(plan.edits().isEmpty());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void everyPlannedEditTargetsAnEmptySlot() throws Exception {
        // The invariant, over a file that mixes every slot shape: an edit only ever lands on a
        // {NoDevice} slot. Nothing the commander bound - keyboard, HOTAS, mouse - is planned over.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                    <UseBoostJuice>
                        <Primary Device="T16000MTHROTTLE" DeviceIndex="1" Key="Joy_POV1Right" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </UseBoostJuice>
                    <CycleFireGroupNext>
                        <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                        <Secondary Device="044F0404" DeviceIndex="0" Key="Joy_4" />
                    </CycleFireGroupNext>
                    <LandingGearToggle>
                        <Primary Device="Mouse" Key="Mouse_4" />
                        <Secondary Device="Keyboard" Key="Key_L" />
                    </LandingGearToggle>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertFalse(plan.edits().isEmpty());
        for (PlannedEdit edit : plan.edits()) {
            ReadOnlyBindingSlots target = slots.get(edit.bindingId());
            KeyBindingsParser.ReadOnlyBindingSlot slot = edit.slotType() == BindingSlotType.PRIMARY
                    ? target.primary()
                    : target.secondary();
            assertEquals("{NoDevice}", slot.device(),
                    "planned an edit over an existing assignment on " + edit.bindingId());
        }
    }

    @Test
    void noAssignmentIsEverAModifierOnItsOwn() throws Exception {
        // Rule: a binding is a main key, optionally with modifiers - never "Left Alt" alone.
        StringBuilder xml = new StringBuilder("<Root>\n");
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            xml.append("<").append(command.getGameBinding()).append(">")
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</").append(command.getGameBinding()).append(">\n");
        }
        xml.append("</Root>\n");

        Plan plan = planEveryControl(parse(xml.toString()), GameKeyboardLayout.OTHER);

        assertFalse(plan.edits().isEmpty());
        for (PlannedEdit edit : plan.edits()) {
            assertFalse(BindingModifier.isSupportedKeyboardModifier("Keyboard", edit.key()),
                    "modifier assigned as the main key of " + edit.bindingId() + ": " + edit.key());
        }
    }

    @Test
    void everyAssignmentIsARealKeyNeverTheNumpadAndNeverABlankModifier() throws Exception {
        // Drains the whole pool by asking for a key for every control EliteIntel drives.
        StringBuilder xml = new StringBuilder("<Root>\n");
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            xml.append("<").append(command.getGameBinding()).append(">")
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</").append(command.getGameBinding()).append(">\n");
        }
        xml.append("</Root>\n");

        Plan plan = planEveryControl(parse(xml.toString()), GameKeyboardLayout.OTHER);

        assertFalse(plan.edits().isEmpty());
        for (PlannedEdit edit : plan.edits()) {
            assertNotNull(edit.key());
            assertFalse(edit.key().isBlank(), "blank main key on " + edit.bindingId());
            assertFalse(edit.key().startsWith("Key_Numpad"),
                    "numpad auto-assigned to " + edit.bindingId() + ": " + edit.key());
            if (edit.modifier() != null) {
                assertTrue(edit.modifier().isSupportedKeyboardModifier(),
                        "blank or unsupported modifier on " + edit.bindingId() + ": " + edit.modifier());
            }
        }
    }

    @Test
    void alreadyKeyboardBoundActionIsNeitherEditedNorSkipped() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <DeployHeatSink>
                        <Primary Device="Keyboard" Key="Key_H" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </DeployHeatSink>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertTrue(plan.edits().isEmpty());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void joystickAxisActionsAreNotTargeted() throws Exception {
        // Axes use <Binding>/<Inverted>/<Deadzone> and have no Primary/Secondary,
        // so the parser excludes them entirely - nothing to assign.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <YawAxisRaw>
                        <Binding Device="T16000M" DeviceIndex="0" Key="Joy_RZAxis" />
                        <Inverted Value="0" />
                        <Deadzone Value="0.00000000" />
                    </YawAxisRaw>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertFalse(slots.containsKey("YawAxisRaw"));
        assertTrue(plan.edits().isEmpty());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void occupiedChordIsSkippedAndNextFreeChordIsUsed() throws Exception {
        SafeKeyboardKeys.Chord first = SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER).get(0);
        SafeKeyboardKeys.Chord second = SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER).get(1);

        // Pre-occupy the very first chord on an unrelated keyboard binding.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <AlreadyBound>
                        <Primary Device="Keyboard" Key="%s">
                            <Modifier Device="Keyboard" Key="%s" />
                        </Primary>
                    </AlreadyBound>
                    <NeedsKey>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </NeedsKey>
                </Root>
                """.formatted(first.key(), first.modifier().key()));

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(1, plan.edits().size());
        PlannedEdit edit = plan.edits().get(0);
        assertEquals("NeedsKey", edit.bindingId());
        assertEquals(second.key(), edit.key());
        assertEquals(second.modifier(), edit.modifier());
    }

    @Test
    void chordsAreNotReusedWithinOneBatch() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <ActionOne>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ActionOne>
                    <ActionTwo>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ActionTwo>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(2, plan.edits().size());
        Set<SafeKeyboardKeys.Chord> used = new HashSet<>();
        for (PlannedEdit edit : plan.edits()) {
            assertTrue(used.add(new SafeKeyboardKeys.Chord(edit.key(), edit.modifier())),
                    "chord reused within batch");
        }
    }

    @Test
    void exhaustedPoolReportsNoFreeKey() throws Exception {
        // Occupy every safe chord, then add one more unbound action.
        StringBuilder xml = new StringBuilder("<Root>\n");
        int i = 0;
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER)) {
            xml.append("<Occupier").append(i++).append(">\n");
            if (chord.hasModifier()) {
                xml.append("<Primary Device=\"Keyboard\" Key=\"").append(chord.key()).append("\">\n")
                        .append("<Modifier Device=\"Keyboard\" Key=\"").append(chord.modifier().key()).append("\" />\n")
                        .append("</Primary>\n");
            } else {
                xml.append("<Primary Device=\"Keyboard\" Key=\"").append(chord.key()).append("\" />\n");
            }
            xml.append("</Occupier").append(i - 1).append(">\n");
        }
        xml.append("<LeftOver>\n<Primary Device=\"{NoDevice}\" Key=\"\" />\n<Secondary Device=\"{NoDevice}\" Key=\"\" />\n</LeftOver>\n");
        xml.append("</Root>\n");

        Map<String, ReadOnlyBindingSlots> slots = parse(xml.toString());

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertTrue(plan.edits().isEmpty());
        assertEquals(List.of(new SkippedBinding("LeftOver", SkipReason.NO_FREE_KEY)), plan.skipped());
    }

    @Test
    void planOneAssignsASingleBinding() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <ActionOne>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ActionOne>
                    <ActionTwo>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ActionTwo>
                </Root>
                """);

        Plan plan = assigner.planOne("ActionTwo", slots, GameKeyboardLayout.OTHER);

        assertEquals(1, plan.edits().size());
        assertEquals("ActionTwo", plan.edits().get(0).bindingId());
    }

    @Test
    void planOneOnAlreadyBoundReturnsEmptyPlan() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <DeployHeatSink>
                        <Primary Device="Keyboard" Key="Key_H" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </DeployHeatSink>
                </Root>
                """);

        Plan plan = assigner.planOne("DeployHeatSink", slots, GameKeyboardLayout.OTHER);

        assertTrue(plan.edits().isEmpty());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void everyPlannedChordComesFromTheSafePool() throws Exception {
        // Enough targets to spill past the combo region into the plain-key fallback,
        // proving the planner only ever draws from the safe pool (which excludes
        // Q/W/A/Z/M/Y, punctuation, and RightAlt - see SafeKeyboardKeysTest).
        StringBuilder xml = new StringBuilder("<Root>\n");
        for (int i = 0; i < 230; i++) {
            xml.append("<Action").append(i).append('>')
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</Action").append(i).append(">\n");
        }
        xml.append("</Root>\n");
        Map<String, ReadOnlyBindingSlots> slots = parse(xml.toString());

        Set<SafeKeyboardKeys.Chord> pool = Set.copyOf(SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER));
        List<PlannedEdit> edits = planEveryControl(slots, GameKeyboardLayout.OTHER).edits();

        assertEquals(230, edits.size());
        for (PlannedEdit edit : edits) {
            assertTrue(
                    pool.contains(new SafeKeyboardKeys.Chord(edit.key(), edit.modifier())),
                    "planned chord outside the safe pool: " + edit.key() + " / " + edit.modifier());
        }
    }

    @Test
    void theGameMenuIsLeftUnboundAndItsKeyIsTakenOutOfThePool() throws Exception {
        // Pause on Key_P: P is spent, with or without modifiers, so nothing else may be given it.
        StringBuilder xml = new StringBuilder("""
                <Root>
                    <Pause>
                        <Primary Device="Keyboard" Key="Key_P" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </Pause>
                """);
        for (int i = 0; i < 40; i++) {
            xml.append("<Action").append(i).append('>')
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</Action").append(i).append(">\n");
        }
        xml.append("</Root>\n");

        Plan plan = planEveryControl(parse(xml.toString()), GameKeyboardLayout.OTHER);

        assertEquals(40, plan.edits().size());
        for (PlannedEdit edit : plan.edits()) {
            assertNotEquals("Key_P", edit.key(), "the game-menu key was handed to " + edit.bindingId());
        }
    }

    @Test
    void anUnboundGameMenuIsSkippedRatherThanFilled() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <Pause>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </Pause>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        // Escape opens that menu anyway; a key here would be spent for nothing.
        assertEquals(List.of("ToggleCargoScoop"), plan.edits().stream().map(PlannedEdit::bindingId).toList());
        assertEquals(List.of(new SkippedBinding("Pause", SkipReason.GAME_MENU_LEFT_UNBOUND)), plan.skipped());

        // And the same answer through the per-row button, which is where the commander asks for it.
        assertEquals(List.of(new SkippedBinding("Pause", SkipReason.GAME_MENU_LEFT_UNBOUND)),
                assigner.planOne("Pause", slots, GameKeyboardLayout.OTHER).skipped());
    }

    @Test
    void ejectAllCargoIsSkippedRatherThanFilled() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <EjectAllCargo>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </EjectAllCargo>
                    <EjectAllCargo_Buggy>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </EjectAllCargo_Buggy>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        // Emptying the hold into space cannot be undone, and no built-in command presses it.
        assertEquals(List.of("ToggleCargoScoop"), plan.edits().stream().map(PlannedEdit::bindingId).toList());
        assertEquals(List.of(
                        new SkippedBinding("EjectAllCargo", SkipReason.LEFT_UNBOUND_ON_PURPOSE),
                        new SkippedBinding("EjectAllCargo_Buggy", SkipReason.LEFT_UNBOUND_ON_PURPOSE)),
                plan.skipped());

        // And the same answer through the per-row button, which is where the commander asks for it.
        assertEquals(List.of(new SkippedBinding("EjectAllCargo", SkipReason.LEFT_UNBOUND_ON_PURPOSE)),
                assigner.planOne("EjectAllCargo", slots, GameKeyboardLayout.OTHER).skipped());
    }

    @Test
    void bothGameMenuSlotsAreTakenOutOfThePool() throws Exception {
        // Only the Primary reaches the executable map, but both keys open the menu.
        StringBuilder xml = new StringBuilder("""
                <Root>
                    <Pause>
                        <Primary Device="Keyboard" Key="Key_P" />
                        <Secondary Device="Keyboard" Key="Key_G" />
                    </Pause>
                """);
        for (int i = 0; i < 40; i++) {
            xml.append("<Action").append(i).append('>')
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</Action").append(i).append(">\n");
        }
        xml.append("</Root>\n");

        Plan plan = planEveryControl(parse(xml.toString()), GameKeyboardLayout.OTHER);

        for (PlannedEdit edit : plan.edits()) {
            assertNotEquals("Key_P", edit.key());
            assertNotEquals("Key_G", edit.key());
        }
    }

    /**
     * Support bundle of 2026-09-30: UI Focus (hold) on bare Left Shift, and a file full of auto-fixed
     * Shift chords on ship controls - each of which fired UI Focus before its own key landed.
     */
    @Test
    void aModifierBoundOnItsOwnIsNeverHandedOutToAControlLiveBesideIt() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <UIFocus>
                        <Primary Device="Keyboard" Key="Key_LeftShift" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </UIFocus>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                    <LandingGearToggle>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </LandingGearToggle>
                    <UseBoostJuice>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </UseBoostJuice>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(3, plan.edits().size());
        for (PlannedEdit edit : plan.edits()) {
            assertNotEquals("Key_LeftShift", edit.modifier() == null ? null : edit.modifier().key(),
                    edit.bindingId() + " was given Left Shift, which fires UI Focus");
        }
    }

    @Test
    void aModifierBoundOnItsOwnInAnotherVehicleStaysInThePool() throws Exception {
        // On-foot Sprint on Left Shift never fires in the ship, so Shift stays available there.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <HumanoidSprintButton>
                        <Primary Device="Keyboard" Key="Key_LeftShift" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </HumanoidSprintButton>
                    <LandingGearToggle>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </LandingGearToggle>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                </Root>
                """);

        Plan plan = planEveryControl(slots, GameKeyboardLayout.OTHER);

        assertEquals(2, plan.edits().size());
        assertEquals("Key_LeftShift", plan.edits().get(1).modifier().key());
    }

    @Test
    void onlyTheRequiredControlsAreFilled() throws Exception {
        // Fix-all fills what VEGA presses and nothing else: an emote nothing presses gains nothing from a key,
        // and a commander later asked who had bound two hundred controls they never chose.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <AAA_Emote>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </AAA_Emote>
                    <ToggleCargoScoop>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </ToggleCargoScoop>
                </Root>
                """);

        Plan plan = assigner.planAll(slots, Set.of("ToggleCargoScoop"), GameKeyboardLayout.OTHER);

        assertEquals(List.of("ToggleCargoScoop"), plan.edits().stream().map(PlannedEdit::bindingId).toList());
        assertTrue(plan.skipped().isEmpty(), "a control outside the required set is not a skip, it is not a target");
        assertEquals(SafeKeyboardKeys.orderedChords(GameKeyboardLayout.OTHER).get(0).key(), plan.edits().get(0).key());
    }

    @Test
    void aRowCanStillBeFilledOutsideTheRequiredSet() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <AAA_Emote>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </AAA_Emote>
                </Root>
                """);

        assertEquals(1, assigner.planOne("AAA_Emote", slots, GameKeyboardLayout.OTHER).edits().size());
    }

    @Test
    void everyControlVegaPressesFitsTheStrictAzertyPool() throws Exception {
        // The strict AZERTY pool is small. On a binds file with no keyboard binding at all (HOTAS or gamepad
        // only), it must still cover every built-in control VEGA presses.
        StringBuilder xml = new StringBuilder("<Root>\n<KeyboardLayout>fr-FR</KeyboardLayout>\n");
        Set<String> written = new HashSet<>();
        Set<String> vegaControls = new HashSet<>();
        for (Bindings.GameCommand command : Bindings.GameCommand.values()) {
            if (command.isDrivenByApp()) {
                vegaControls.add(command.getGameBinding());
            }
            if (written.add(command.getGameBinding())) {
                xml.append("<").append(command.getGameBinding()).append(">")
                        .append("<Primary Device=\"{NoDevice}\" Key=\"\" />")
                        .append("<Secondary Device=\"{NoDevice}\" Key=\"\" />")
                        .append("</").append(command.getGameBinding()).append(">\n");
            }
        }
        xml.append("</Root>\n");

        Plan plan = assigner.planAll(parse(xml.toString()), vegaControls, GameKeyboardLayout.AZERTY);

        List<String> outOfKeys = plan.skipped().stream()
                .filter(skip -> skip.reason() == SkipReason.NO_FREE_KEY)
                .map(SkippedBinding::bindingId)
                .toList();
        assertEquals(List.of(), outOfKeys, "controls VEGA presses ran out of keys");
        assertTrue(plan.edits().stream().allMatch(edit -> vegaControls.contains(edit.bindingId())));
    }

    @Test
    void anAzertyFileIsOnlyGivenKeysItCanName() throws Exception {
        StringBuilder xml = new StringBuilder("<Root>\n<KeyboardLayout>fr-FR</KeyboardLayout>\n");
        for (int i = 0; i < 400; i++) {
            xml.append("<Action").append(i).append(">")
                    .append("<Primary Device=\"{NoDevice}\" Key=\"\" /><Secondary Device=\"{NoDevice}\" Key=\"\" />")
                    .append("</Action").append(i).append(">\n");
        }
        xml.append("</Root>");

        Plan plan = planEveryControl(parse(xml.toString()), GameKeyboardLayout.AZERTY);

        assertFalse(plan.edits().isEmpty());
        for (PlannedEdit edit : plan.edits()) {
            assertTrue(GameKeyboardLayout.AZERTY.namesKey(edit.key()), "AZERTY file handed " + edit.key());
            assertFalse(edit.key().matches("Key_[0-9]"), "AZERTY file handed a top-row digit: " + edit.key());
        }
    }

    @Test
    void anAzertyBindingOnADigitCountsAsMissingAndGetsTheEmptySlotFilled() throws Exception {
        // What an older auto-assign left in a fr-FR file: Hyperspace on Shift+Key_9. The slot is kept as it is
        // (never replaced), but it is not pressable, so the control is missing and the empty Secondary is filled.
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <KeyboardLayout>fr-FR</KeyboardLayout>
                    <Hyperspace>
                        <Primary Device="Keyboard" Key="Key_9">
                            <Modifier Device="Keyboard" Key="Key_LeftShift" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </Hyperspace>
                </Root>
                """);

        assertFalse(slots.get("Hyperspace").primary().keyboardUsable());
        assertFalse(assigner.isKeyboardBound(slots.get("Hyperspace")));

        Plan plan = planEveryControl(slots, GameKeyboardLayout.AZERTY);

        assertEquals(1, plan.edits().size());
        assertEquals(BindingSlotType.SECONDARY, plan.edits().get(0).slotType());
        assertTrue(GameKeyboardLayout.AZERTY.namesKey(plan.edits().get(0).key()));
    }

    @Test
    void aDigitIsStillPressableOutsideAnAzertyFile() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <KeyboardLayout>en-US</KeyboardLayout>
                    <Hyperspace>
                        <Primary Device="Keyboard" Key="Key_9" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </Hyperspace>
                </Root>
                """);

        assertTrue(slots.get("Hyperspace").primary().keyboardUsable());
    }

    /**
     * Plans with every control in the file required, for the tests about how a key is chosen.
     */
    private Plan planEveryControl(Map<String, ReadOnlyBindingSlots> slots, GameKeyboardLayout layout) {
        return assigner.planAll(slots, slots.keySet(), layout);
    }

    private Map<String, ReadOnlyBindingSlots> parse(String xml) throws Exception {
        Path file = tempDir.resolve("test-" + System.nanoTime() + ".binds");
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return parser.parseReadOnlyBindingSlots(file.toFile());
    }
}
