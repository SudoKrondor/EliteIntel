package elite.intel.bindforge.devices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private void binds(String name, String content) throws IOException {
        Files.writeString(bindings.resolve(name), content);
    }
}
