package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the bindings name is the difference between a divergence that breaks the game and one that is only
 * untidy, so the reading of it has to be exact about which values are entry names and which are not.
 */
class BindsDeviceReferencesTest {

    @TempDir
    Path bindings;

    @Test
    void namesADeviceABindingUses() throws IOException {
        binds("Custom.4.2.binds", """
                <Root>
                  <LandingGearToggle>
                    <Primary Device="RVWAP" Key="Joy_9" />
                    <Secondary Device="{NoDevice}" Key="" />
                  </LandingGearToggle>
                </Root>
                """);

        assertEquals(Set.of("RVWAP"), BindsDeviceReferences.referencedEntryNames(bindings));
    }

    /** Keyboard and mouse are named directly and have no entry, so they are not entries. */
    @Test
    void keyboardMouseAndEmptySlotsAreNotEntries() throws IOException {
        binds("Custom.4.2.binds", """
                <Root>
                  <A><Primary Device="Keyboard" Key="Key_A" /></A>
                  <B><Primary Device="Mouse" Key="Mouse_1" /></B>
                  <C><Primary Device="{NoDevice}" Key="" /></C>
                </Root>
                """);

        assertTrue(BindsDeviceReferences.referencedEntryNames(bindings).isEmpty());
    }

    /**
     * A binding falls back to raw VID+PID hex when no entry matches. That is a device with no entry at all -
     * a different problem from a divergent one - so it is not counted as an entry name.
     */
    @Test
    void rawHardwareIdsAreNotEntryNames() throws IOException {
        binds("Custom.4.2.binds", """
                <Root>
                  <A><Primary Device="334403F3" Key="Joy_1" /></A>
                  <B><Primary Device="T-Rudder" Key="Joy_2" /></B>
                </Root>
                """);

        Set<String> names = BindsDeviceReferences.referencedEntryNames(bindings);

        assertEquals(Set.of("T-Rudder"), names);
        assertFalse(names.contains("334403F3"));
    }

    /**
     * The game validates every file in the folder, not just the active preset, so a device named only in a
     * preset the user is not currently using still counts.
     */
    @Test
    void readsEveryPresetInTheFolder() throws IOException {
        binds("Custom.4.2.binds", "<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");
        binds("Racing.4.2.binds", "<Root><A><Primary Device=\"VPCThrottle\" Key=\"Joy_2\" /></A></Root>");

        assertEquals(Set.of("RVWAP", "VPCThrottle"), BindsDeviceReferences.referencedEntryNames(bindings));
    }

    /**
     * The folder demonstrably holds files with a .binds name that are not binds files, most often a
     * StartPreset picked by mistake. One of those must not stop the rest being read.
     */
    @Test
    void aFileThatIsNotBindsDoesNotStopTheOthersBeingRead() throws IOException {
        binds("Broken.4.2.binds", "this is a StartPreset, saved under the wrong name");
        binds("Custom.4.2.binds", "<Root><A><Primary Device=\"RVWAP\" Key=\"Joy_1\" /></A></Root>");

        assertEquals(Set.of("RVWAP"), BindsDeviceReferences.referencedEntryNames(bindings));
    }

    @Test
    void aFolderWithNoBindsGivesNothing() throws IOException {
        assertTrue(BindsDeviceReferences.referencedEntryNames(bindings).isEmpty());
        assertTrue(BindsDeviceReferences.referencedEntryNames(bindings.resolve("gone")).isEmpty());
    }

    /** Each slot naming the device counts, across every preset - what CLEAR's refusal says is at stake (A10). */
    @Test
    void countsEverySlotNamingADeviceAcrossThePresets() throws IOException {
        binds("Custom.4.2.binds", """
                <Root>
                  <A><Primary Device="LVWAP" Key="Joy_1" /><Secondary Device="LVWAP" Key="Joy_2" /></A>
                  <B><Primary Device="Keyboard" Key="Key_B" /></B>
                </Root>
                """);
        binds("Racing.4.2.binds", "<Root><A><Primary Device=\"LVWAP\" Key=\"Joy_3\" /></A></Root>");

        assertEquals(Map.of("LVWAP", 3), BindsDeviceReferences.referenceCounts(bindings));
    }

    /**
     * Review, 2026-10-11: the counts gate removals, so a bindings folder that is not there must not read as
     * "nothing names this device" - that would let CLEAR and Apply strip an entry the game needs.
     */
    @Test
    void countsRefuseAFolderThatIsNotThere() {
        assertThrows(IOException.class, () -> BindsDeviceReferences.referenceCounts(bindings.resolve("moved")));
    }

    @Test
    void countsRefuseAFolderWhoseBindsAllFailToParse() throws IOException {
        binds("Custom.4.2.binds", "<Root><unclosed></Root>");

        assertThrows(IOException.class, () -> BindsDeviceReferences.referenceCounts(bindings));
    }

    /** An empty folder is an answer, not a failure: no custom preset names anything. */
    @Test
    void countsOfAnEmptyFolderAreEmpty() throws IOException {
        assertTrue(BindsDeviceReferences.referenceCounts(bindings).isEmpty());
    }

    /** Severity stays lenient: a missing folder only turns findings yellow. */
    @Test
    void namesOfAFolderThatIsNotThereAreEmpty() throws IOException {
        assertTrue(BindsDeviceReferences.referencedEntryNames(bindings.resolve("moved")).isEmpty());
    }

    /**
     * Hardware ids are counted on their own, keyed by the value exactly as written, so a rewrite can be built from
     * what was found (A6b, 2026-10-14).
     */
    @Test
    void hardwareIdsAreCountedAsWritten() throws IOException {
        binds("Custom.4.2.binds", """
                <Root>
                  <A><Primary Device="334483F4" Key="Joy_1" /><Secondary Device="334483F4" Key="Joy_2" /></A>
                  <B><Binding Device="334483f4" Key="Joy_XAxis" /></B>
                  <C><Primary Device="T-Rudder" Key="Joy_2" /></C>
                </Root>
                """);

        assertEquals(Map.of("334483F4", 2, "334483f4", 1), BindsDeviceReferences.hardwareIdCounts(bindings));
        assertEquals(Map.of("T-Rudder", 1), BindsDeviceReferences.referenceCounts(bindings),
                "entry names are unaffected");
    }

    @Test
    void hardwareIdCountsRefuseAMissingFolder() {
        assertThrows(IOException.class, () -> BindsDeviceReferences.hardwareIdCounts(bindings.resolve("gone")));
    }

    /** Read in either order and either case, as format §4.2 asks of a reader - and only this device's. */
    @Test
    void hardwareIdsForADeviceMatchEitherOrderAndCase() {
        Map<String, Integer> counts = Map.of("334483F4", 2, "83f43344", 1, "334403F5", 7);

        assertEquals(Map.of("334483F4", 2, "83f43344", 1),
                BindsDeviceReferences.hardwareIdsFor("3344", "83F4", counts));
        assertTrue(BindsDeviceReferences.hardwareIdsFor("1234", "0001", counts).isEmpty());
    }

    private void binds(String name, String content) throws IOException {
        Files.writeString(bindings.resolve(name), content);
    }

    /** A hand-edited .binds can spell a name otherwise; several names of one device count each binding once. */
    @Test
    void bindingsNamingCountsEveryNameIgnoringCase() {
        Map<String, Integer> counts = Map.of("LVWAP", 3, "lvwap", 2, "LeftStick", 4, "RVWAP", 7);

        assertEquals(5, BindsDeviceReferences.bindingsNaming(Set.of("LVWAP"), counts));
        assertEquals(9, BindsDeviceReferences.bindingsNaming(List.of("LeftStick", "LVWAP", "Lvwap"), counts));
        assertEquals(0, BindsDeviceReferences.bindingsNaming(Collections.singletonList(null), counts));
    }
}
