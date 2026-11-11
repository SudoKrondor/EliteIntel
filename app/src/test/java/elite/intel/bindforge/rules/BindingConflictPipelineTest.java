package elite.intel.bindforge.rules;

import elite.intel.bindforge.io.BindingsMonitor;
import elite.intel.bindforge.io.KeyBindingsParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the parse-and-scan half of what {@link BindingsMonitor#blockingConflicts()} runs - a real
 * {@code .binds} file on disk, through {@link KeyBindingsParser#parseBindingSlots(File)}, into
 * {@link BindingConflictScanner#scanSlots} - rather than the scanner alone. The sibling startup
 * scan that reads the same slot map, {@link ReservedKeyChords}, gets one file-backed case for the
 * same reason.
 * <p>
 * It stops at the scanner: {@code BindingsMonitor} itself, the Bindings tab, the assign dialog and
 * the keyboard map view are still untested, and those are where a rewiring mistake would recur.
 * <p>
 * WHY it is worth a file and not another key-set fixture: the defect was never in the comparison. It
 * was in what reached the comparison. Every slot pair survived the parse and then one of the two was
 * dropped on the way, so a scanner that was correct in isolation still missed the clash in the app.
 * A test that hands the scanner its key-sets directly cannot see that.
 */
class BindingConflictPipelineTest {

    @TempDir
    Path tempDir;

    /**
     * A file whose only clash lives across slots: {@code UI_Up} holds Up-arrow in Primary and W in
     * Secondary, and W is also the map camera's Primary. Elite fires both on W.
     */
    private static final String CROSS_SLOT_CLASH = """
            	<UI_Up>
            		<Primary Device="Keyboard" Key="Key_UpArrow" />
            		<Secondary Device="Keyboard" Key="Key_W" />
            	</UI_Up>
            	<CamPitchUp>
            		<Primary Device="Keyboard" Key="Key_W" />
            		<Secondary Device="{NoDevice}" Key="" />
            	</CamPitchUp>
            """;

    private File bindsFile(String body) throws Exception {
        Path file = tempDir.resolve("Custom.4.2.binds");
        Files.writeString(file, "<Root PresetName=\"Test\" MajorVersion=\"4\" MinorVersion=\"2\">\n"
                + body
                + "\n</Root>\n");
        return file.toFile();
    }

    @Test
    void aChordSharedBetweenOnePrimaryAndAnotherSecondaryIsFoundInARealFile() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots =
                KeyBindingsParser.getInstance().parseBindingSlots(bindsFile(CROSS_SLOT_CLASH));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanSlots(slots);

        assertEquals(1, conflicts.size(), "the cross-slot clash on W should be reported once: " + conflicts);
        BindingConflictScanner.Conflict conflict = conflicts.get(0);
        assertEquals("CamPitchUp", conflict.actionA());
        assertEquals("UI_Up", conflict.actionB());
        assertTrue(conflict.blocking(),
                "map camera against UI navigation stops EliteIntel driving the game, so it must be blocking");
    }

    /**
     * Two actions can collide on more than one chord, and each collision is its own line to fix.
     * <p>
     * WHY it is worth pinning: the de-dupe key is the pair <em>and</em> the chord, so this is two
     * conflicts by design, not one reported twice. A de-dupe keyed on the pair alone would look
     * correct on every other test in this file and would silently hide the second chord.
     */
    @Test
    void aPairCollidingOnTwoChordsIsReportedOncePerChord() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots = KeyBindingsParser.getInstance().parseBindingSlots(bindsFile("""
                	<UI_Up>
                		<Primary Device="Keyboard" Key="Key_W" />
                		<Secondary Device="Keyboard" Key="Key_X" />
                	</UI_Up>
                	<CamPitchUp>
                		<Primary Device="Keyboard" Key="Key_W" />
                		<Secondary Device="Keyboard" Key="Key_X" />
                	</CamPitchUp>
                """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanSlots(slots);

        assertEquals(2, conflicts.size(), "one conflict per shared chord: " + conflicts);
        assertEquals(List.of(Set.of("Key_W"), Set.of("Key_X")),
                conflicts.stream().map(BindingConflictScanner.Conflict::chord).sorted(
                        Comparator.comparing(c -> String.join("", c))).toList(),
                "both W and X should be named, so the commander fixes both");
        assertTrue(conflicts.stream().allMatch(BindingConflictScanner.Conflict::blocking));
    }

    /**
     * The support bundle of 2026-09-30, cut down to the two controls involved: Galaxy Map's keyboard
     * chord sits in the Secondary slot behind a joystick axis, and its Left Shift is UI Focus on its own.
     */
    @Test
    void aModifierBoundOnItsOwnIsFoundAgainstASecondarySlotChordInARealFile() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots = KeyBindingsParser.getInstance().parseBindingSlots(bindsFile("""
                	<UIFocus>
                		<Primary Device="Keyboard" Key="Key_LeftShift" />
                		<Secondary Device="231D3200" Key="Joy_5" />
                	</UIFocus>
                	<UIFocusMode Value="Bindings_FocusModeHold" />
                	<GalaxyMapOpen>
                		<Primary Device="SaitekX56Throttle" Key="Pos_Joy_UAxis" />
                		<Secondary Device="Keyboard" Key="Key_J">
                			<Modifier Device="Keyboard" Key="Key_LeftShift" />
                			<Modifier Device="Keyboard" Key="Key_LeftControl" />
                		</Secondary>
                	</GalaxyMapOpen>
                """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanSlots(slots);

        assertEquals(1, conflicts.size(), "the shadowed Left Shift should be reported once: " + conflicts);
        BindingConflictScanner.Conflict conflict = conflicts.get(0);
        assertEquals("GalaxyMapOpen", conflict.actionA());
        assertEquals(Set.of("Key_J", "Key_LeftShift", "Key_LeftControl"), conflict.chordA());
        assertEquals("UIFocus", conflict.actionB());
        assertEquals(Set.of("Key_LeftShift"), conflict.chordB());
    }

    /**
     * A tap and a long press on one key are two controls (FP-1), read from the file: the scanner's own tests hand it
     * the hold flag directly, so only a file-backed case shows that {@code <Hold Value="1" />} survives the parse.
     * Two long presses on that key still clash.
     */
    @Test
    void aPressAndALongPressOnOneKeyAreTwoControlsInARealFile() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots = KeyBindingsParser.getInstance().parseBindingSlots(bindsFile("""
                	<HumanoidSelectPrimaryWeaponButton>
                		<Primary Device="Keyboard" Key="Key_1" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</HumanoidSelectPrimaryWeaponButton>
                	<HumanoidHealthPack>
                		<Primary Device="Keyboard" Key="Key_1">
                			<Hold Value="1" />
                		</Primary>
                		<Secondary Device="{NoDevice}" Key="" />
                	</HumanoidHealthPack>
                	<HumanoidBattery>
                		<Primary Device="{NoDevice}" Key="" />
                		<Secondary Device="Keyboard" Key="Key_1">
                			<Hold Value="1" />
                		</Secondary>
                	</HumanoidBattery>
                """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanSlots(slots);

        assertEquals(1, conflicts.size(), "only the two long presses on 1 should clash: " + conflicts);
        assertEquals("HumanoidBattery", conflicts.get(0).actionA());
        assertEquals("HumanoidHealthPack", conflicts.get(0).actionB());
    }

    /**
     * FN-6: a keyboard key with its modifier held on a controller is a chord the game fires, and two actions on it
     * clash. The Bindings tab's scan reads it; the execution view, which feeds the spoken startup warning, still
     * drops it because EliteIntel cannot press it.
     */
    @Test
    void aKeyWithAControllerModifierIsScannedOnTheBindingsTab() throws Exception {
        File file = bindsFile("""
                	<LandingGearToggle>
                		<Primary Device="Keyboard" Key="Key_K">
                			<Modifier Device="231D3200" Key="Joy_1" />
                		</Primary>
                		<Secondary Device="{NoDevice}" Key="" />
                	</LandingGearToggle>
                	<ToggleCargoScoop>
                		<Primary Device="{NoDevice}" Key="" />
                		<Secondary Device="Keyboard" Key="Key_K">
                			<Modifier Device="231D3200" Key="Joy_1" />
                		</Secondary>
                	</ToggleCargoScoop>
                """);

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanReadOnlySlots(
                KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(file));

        assertEquals(1, conflicts.size(), "the shared controller-modified K should be reported once: " + conflicts);
        assertEquals("LandingGearToggle", conflicts.get(0).actionA());
        assertEquals("ToggleCargoScoop", conflicts.get(0).actionB());
        assertEquals(Set.of("Key_K", "231D3200/JOY_1"), conflicts.get(0).chord());
        assertTrue(BindingConflictScanner.scanSlots(KeyBindingsParser.getInstance().parseBindingSlots(file)).isEmpty(),
                "the execution view keeps dropping a slot EliteIntel cannot press");
    }

    /**
     * The controller modifier is named with its device: {@code Joy_1} on two sticks is two buttons, and neither
     * chord is the bare keyboard key.
     */
    @Test
    void aControllerModifierOnAnotherDeviceOrNoneIsAnotherChord() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<LandingGearToggle>
                        		<Primary Device="Keyboard" Key="Key_K">
                        			<Modifier Device="231D3200" Key="Joy_1" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</LandingGearToggle>
                        	<ToggleCargoScoop>
                        		<Primary Device="Keyboard" Key="Key_K">
                        			<Modifier Device="044F0404" Key="Joy_1" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</ToggleCargoScoop>
                        	<DeployHardpointToggle>
                        		<Primary Device="Keyboard" Key="Key_K" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</DeployHardpointToggle>
                        """));

        assertTrue(BindingConflictScanner.scanReadOnlySlots(slots).isEmpty(),
                "three different chords on K: " + BindingConflictScanner.scanReadOnlySlots(slots));
    }

    /**
     * A reserved chord in a Secondary slot is found from the file, not only from a hand-built map.
     */
    @Test
    void aReservedChordInASecondarySlotIsFoundByTheReservedScan() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots = KeyBindingsParser.getInstance().parseBindingSlots(bindsFile("""
                	<GalaxyMapOpen>
                		<Primary Device="Keyboard" Key="Key_M" />
                		<Secondary Device="Keyboard" Key="Key_F4">
                			<Modifier Device="Keyboard" Key="Key_LeftAlt" />
                		</Secondary>
                	</GalaxyMapOpen>
                """));

        List<ReservedKeyChords.ReservedBinding> found = ReservedKeyChords.scan(slots);

        assertEquals(1, found.size(), "Alt+F4 in the Secondary closes the game: " + found);
        assertEquals("GalaxyMapOpen", found.get(0).action());
        assertEquals(ReservedKeyChords.Rule.OS_CLAIMED, found.get(0).rule());
    }

    /**
     * The keyboard filter still runs ahead of the scan: a HOTAS button is not a chord we can collide
     * with, and must not be compared as one just because both slots are now read.
     */
    @Test
    void aControllerSlotIsNotComparedAsAChord() throws Exception {
        Map<String, KeyBindingsParser.BindingSlots> slots = KeyBindingsParser.getInstance().parseBindingSlots(bindsFile("""
                	<UI_Up>
                		<Primary Device="T16000MTHROTTLE" DeviceIndex="0" Key="Joy_5" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</UI_Up>
                	<CamPitchUp>
                		<Primary Device="T16000MTHROTTLE" DeviceIndex="0" Key="Joy_5" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</CamPitchUp>
                """));

        assertTrue(BindingConflictScanner.scanSlots(slots).isEmpty(),
                "two actions on the same joystick button is Elite's business, not a keyboard conflict");
    }

    /**
     * D6 (Krondor, 2026-11-10): the Bindings tab's full-file scan compares a slot whose main input is on a controller, as
     * SAVE in the capture dialog always did - two actions on one stick button both fire. The execution view, which
     * feeds the spoken startup warning, still drops them.
     */
    @Test
    void twoActionsOnOneControllerButtonClashOnTheBindingsTab() throws Exception {
        File file = bindsFile("""
                	<LandingGearToggle>
                		<Primary Device="T16000MTHROTTLE" DeviceIndex="0" Key="Joy_5" />
                		<Secondary Device="{NoDevice}" Key="" />
                	</LandingGearToggle>
                	<ToggleCargoScoop>
                		<Primary Device="{NoDevice}" Key="" />
                		<Secondary Device="t16000mthrottle" DeviceIndex="0" Key="Joy_5" />
                	</ToggleCargoScoop>
                """);

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanReadOnlySlots(
                KeyBindingsParser.getInstance().parseReadOnlyBindingSlots(file));

        assertEquals(1, conflicts.size(), "one button, matched ignoring case: " + conflicts);
        assertEquals(Set.of("T16000MTHROTTLE/JOY_5"), conflicts.get(0).chord());
        assertTrue(BindingConflictScanner.scanSlots(KeyBindingsParser.getInstance().parseBindingSlots(file)).isEmpty());
    }

    /** {@code Joy_5} on two controllers is two buttons, and a keyboard key is never a controller's. */
    @Test
    void theSameButtonOnTwoControllersIsTwoInputs() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<LandingGearToggle>
                        		<Primary Device="LVWAP" Key="Joy_5" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</LandingGearToggle>
                        	<ToggleCargoScoop>
                        		<Primary Device="RVWAP" Key="Joy_5" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</ToggleCargoScoop>
                        	<DeployHardpointToggle>
                        		<Primary Device="Keyboard" Key="Key_5" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</DeployHardpointToggle>
                        """));

        assertTrue(BindingConflictScanner.scanReadOnlySlots(slots).isEmpty(),
                "three different inputs: " + BindingConflictScanner.scanReadOnlySlots(slots));
    }

    /** A chord of two buttons on one stick is one input set, in any order; the same chord twice clashes. */
    @Test
    void aChordOfTwoControllerButtonsClashesWithTheSameChord() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<LandingGearToggle>
                        		<Primary Device="LVWAP" Key="Joy_5">
                        			<Modifier Device="LVWAP" Key="Joy_1" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</LandingGearToggle>
                        	<ToggleCargoScoop>
                        		<Primary Device="LVWAP" Key="Joy_1">
                        			<Modifier Device="LVWAP" Key="Joy_5" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</ToggleCargoScoop>
                        """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanReadOnlySlots(slots);

        assertEquals(1, conflicts.size(), "" + conflicts);
        assertEquals(Set.of("LVWAP/JOY_5", "LVWAP/JOY_1"), conflicts.get(0).chord());
    }

    /**
     * The HOTAS shift button: {@code Joy_1} held for {@code Joy_1 + Joy_5}, and bound alone to another action. The
     * game records the button held first as the modifier, and it fires alone first - a shadow (D6). The chord's main
     * button bound alone is not one.
     */
    @Test
    void aShiftButtonBoundAloneShadowsTheChordThatHoldsIt() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<LandingGearToggle>
                        		<Primary Device="LVWAP" Key="Joy_5">
                        			<Modifier Device="LVWAP" Key="Joy_1" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</LandingGearToggle>
                        	<ToggleCargoScoop>
                        		<Primary Device="LVWAP" Key="Joy_1" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</ToggleCargoScoop>
                        	<DeployHardpointToggle>
                        		<Primary Device="LVWAP" Key="Joy_5" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</DeployHardpointToggle>
                        """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanReadOnlySlots(slots);

        assertEquals(1, conflicts.size(), "only the shift button: " + conflicts);
        BindingConflictScanner.Conflict conflict = conflicts.get(0);
        assertEquals("LandingGearToggle", conflict.actionA());
        assertEquals("ToggleCargoScoop", conflict.actionB());
        assertEquals(Set.of("LVWAP/JOY_1"), conflict.chordB());
    }

    /** A controller button held as a keyboard key's modifier (FN-6), and bound alone elsewhere, is a shadow too. */
    @Test
    void aControllerModifierOfAKeyboardChordBoundAloneIsAShadow() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<LandingGearToggle>
                        		<Primary Device="Keyboard" Key="Key_K">
                        			<Modifier Device="231D3200" Key="Joy_1" />
                        		</Primary>
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</LandingGearToggle>
                        	<ToggleCargoScoop>
                        		<Primary Device="231D3200" Key="Joy_1" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</ToggleCargoScoop>
                        """));

        assertEquals(1, BindingConflictScanner.scanReadOnlySlots(slots).size());
    }

    /** A clash on a controller button is never blocking: EliteIntel presses only the keyboard (D6). */
    @Test
    void aClashOnAControllerButtonIsNeverBlocking() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<UI_Select>
                        		<Primary Device="LVWAP" Key="Joy_2" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</UI_Select>
                        	<QuickCommsPanel>
                        		<Primary Device="LVWAP" Key="Joy_2" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</QuickCommsPanel>
                        """));

        List<BindingConflictScanner.Conflict> conflicts = BindingConflictScanner.scanReadOnlySlots(slots);

        assertEquals(1, conflicts.size(), "" + conflicts);
        assertTrue(!conflicts.get(0).blocking());
    }

    /** A ship control and an SRV one on one button are never live together, on a controller as on the keyboard. */
    @Test
    void aShipAndAnSrvControlOnOneButtonNeverClash() throws Exception {
        Map<String, KeyBindingsParser.ReadOnlyBindingSlots> slots = KeyBindingsParser.getInstance()
                .parseReadOnlyBindingSlots(bindsFile("""
                        	<PrimaryFire>
                        		<Primary Device="LVWAP" Key="Joy_1" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</PrimaryFire>
                        	<BuggyPrimaryFireButton>
                        		<Primary Device="LVWAP" Key="Joy_1" />
                        		<Secondary Device="{NoDevice}" Key="" />
                        	</BuggyPrimaryFireButton>
                        """));

        assertTrue(BindingConflictScanner.scanReadOnlySlots(slots).isEmpty());
    }
}
