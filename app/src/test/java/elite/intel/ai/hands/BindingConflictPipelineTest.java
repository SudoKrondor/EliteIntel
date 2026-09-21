package elite.intel.ai.hands;

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
 * {@link BindingConflictScanner#scanSlots} - rather than the scanner alone.
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
}
