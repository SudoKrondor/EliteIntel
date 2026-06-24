package elite.intel.ai.hands;

import elite.intel.ai.hands.KeyBindingsParser.ReadOnlyBindingSlots;
import elite.intel.ai.hands.MissingBindingAutoAssigner.Plan;
import elite.intel.ai.hands.MissingBindingAutoAssigner.PlannedEdit;
import elite.intel.ai.hands.MissingBindingAutoAssigner.SkipReason;
import elite.intel.ai.hands.MissingBindingAutoAssigner.SkippedBinding;
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

        Plan plan = assigner.planAll(slots);

        assertEquals(1, plan.edits().size());
        PlannedEdit edit = plan.edits().get(0);
        assertEquals("ToggleCargoScoop", edit.bindingId());
        assertEquals(BindingSlotType.PRIMARY, edit.slotType());
        // First chord in the pool: first base key + first safe modifier.
        assertEquals(SafeKeyboardKeys.orderedChords().get(0).key(), edit.key());
        assertEquals(SafeKeyboardKeys.orderedChords().get(0).modifier(), edit.modifier());
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

        Plan plan = assigner.planAll(slots);

        assertEquals(1, plan.edits().size());
        assertEquals(BindingSlotType.SECONDARY, plan.edits().get(0).slotType());
    }

    @Test
    void bothControllerSlotsAreSkipped() throws Exception {
        Map<String, ReadOnlyBindingSlots> slots = parse("""
                <Root>
                    <CycleFireGroupNext>
                        <Primary Device="T16000M" DeviceIndex="0" Key="Joy_3" />
                        <Secondary Device="044F0404" DeviceIndex="0" Key="Joy_4" />
                    </CycleFireGroupNext>
                </Root>
                """);

        Plan plan = assigner.planAll(slots);

        assertTrue(plan.edits().isEmpty());
        assertEquals(1, plan.skipped().size());
        assertEquals(SkipReason.BOTH_SLOTS_OCCUPIED, plan.skipped().get(0).reason());
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

        Plan plan = assigner.planAll(slots);

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

        Plan plan = assigner.planAll(slots);

        assertFalse(slots.containsKey("YawAxisRaw"));
        assertTrue(plan.edits().isEmpty());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void occupiedChordIsSkippedAndNextFreeChordIsUsed() throws Exception {
        SafeKeyboardKeys.Chord first = SafeKeyboardKeys.orderedChords().get(0);
        SafeKeyboardKeys.Chord second = SafeKeyboardKeys.orderedChords().get(1);

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

        Plan plan = assigner.planAll(slots);

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

        Plan plan = assigner.planAll(slots);

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
        for (SafeKeyboardKeys.Chord chord : SafeKeyboardKeys.orderedChords()) {
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

        Plan plan = assigner.planAll(slots);

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

        Plan plan = assigner.planOne("ActionTwo", slots);

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

        Plan plan = assigner.planOne("DeployHeatSink", slots);

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

        Set<SafeKeyboardKeys.Chord> pool = Set.copyOf(SafeKeyboardKeys.orderedChords());
        List<PlannedEdit> edits = assigner.planAll(slots).edits();

        assertEquals(230, edits.size());
        for (PlannedEdit edit : edits) {
            assertTrue(
                    pool.contains(new SafeKeyboardKeys.Chord(edit.key(), edit.modifier())),
                    "planned chord outside the safe pool: " + edit.key() + " / " + edit.modifier());
        }
    }

    private Map<String, ReadOnlyBindingSlots> parse(String xml) throws Exception {
        Path file = tempDir.resolve("test-" + System.nanoTime() + ".binds");
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return parser.parseReadOnlyBindingSlots(file.toFile());
    }
}
