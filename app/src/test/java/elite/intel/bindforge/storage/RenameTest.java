package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elite.intel.bindforge.storage.BindingsComparisonTest.binds;
import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Renaming a binds file (bind-editor.md, <em>Shell</em>; storage.md rule 7; Storage S12): one edit changes the file
 * name, its {@code PresetName} and every preset line naming it; a factory BindName is refused whatever its case.
 */
class RenameTest {

    private static final String PRESET = "Custom\r\nCustom\r\nSrvOnly\r\nCustom";

    @Test
    void theFileThePresetNameAndThePresetLinesChangeTogether() throws IOException {
        ConfigurationFiles applied = set(Map.of("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", ""),
                "SrvOnly.4.2.binds", binds("SrvOnly", "Key_B", "Joy_XAxis", ""), "StartPreset.4.start", PRESET));

        ConfigurationFiles renamed = Rename.rename(applied, applied, "Custom.4.2.binds", "DualVirpil");

        assertFalse(renamed.bindings().containsKey("Custom.4.2.binds"));
        assertEquals(binds("DualVirpil", "Key_A", "Joy_XAxis", ""), text(renamed.bindings().get("DualVirpil.4.2.binds")));
        assertEquals(new StartPresetSections("DualVirpil", "DualVirpil", "SrvOnly", "DualVirpil", false),
                PresetEdits.sections(renamed).orElseThrow(), "a line naming another file stays");
        assertArrayEquals(applied.bindings().get("SrvOnly.4.2.binds"), renamed.bindings().get("SrvOnly.4.2.binds"));
    }

    /** Renamed back, the edits are the working copy again and the bar goes dark. */
    @Test
    void renamingBackLeavesTheWorkingCopyAsItWas() throws IOException {
        ConfigurationFiles applied = set(Map.of("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", ""),
                "StartPreset.4.start", "Custom\r\nCustom\r\nCustom\r\nCustom"));

        ConfigurationFiles away = Rename.rename(applied, applied, "Custom.4.2.binds", "Mine");
        ConfigurationFiles back = Rename.rename(away, applied, "Mine.4.2.binds", "Custom");

        assertTrue(back.sameAs(applied));
    }

    @Test
    void theFilesOwnNameChangesNothing() throws IOException {
        ConfigurationFiles applied = set(Map.of("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", "")));

        assertSame(applied, Rename.rename(applied, applied, "Custom.4.2.binds", "Custom"));
    }

    /** Alan, 2026-11-03, D3: on Windows a name differing only in case is the same file. */
    @Test
    void aNameTheSetHoldsIgnoringCaseIsRefused() {
        ConfigurationFiles applied = set(Map.of("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", ""),
                "Spare.4.2.binds", binds("Spare", "Key_A", "Joy_XAxis", "")));

        assertThrows(IllegalArgumentException.class,
                () -> Rename.rename(applied, applied, "Custom.4.2.binds", "spare"));
        assertThrows(IllegalArgumentException.class,
                () -> Rename.rename(applied, applied, "Custom.4.2.binds", "custom"));
    }

    @Test
    void aNameThatIsNotABindNameIsRefused() {
        ConfigurationFiles applied = set(Map.of("Custom.4.2.binds", binds("Custom", "Key_A", "Joy_XAxis", "")));

        assertThrows(IllegalArgumentException.class,
                () -> Rename.rename(applied, applied, "Custom.4.2.binds", "My.Custom"));
        assertThrows(IllegalArgumentException.class,
                () -> Rename.rename(applied, applied, "Missing.4.2.binds", "Mine"));
    }

    /** Alan, 2026-11-03, D4: refused by its characters, whatever their case; not loosely. */
    @Test
    void aFactoryBindNameIsRefusedIgnoringCaseOnly() {
        List<String> factory = List.of("KeyboardMouseOnly", "GamePadDefault");

        assertEquals(Optional.of("KeyboardMouseOnly"), Rename.factoryClash("keyboardmouseonly", factory));
        assertEquals(Optional.empty(), Rename.factoryClash("Keyboard_Mouse_Only", factory));
        assertEquals(Optional.empty(), Rename.factoryClash("KeyboardMouseOnly2", factory));
    }

    @Test
    void aBindsFileNamedLikeAFactoryOneIsFound() {
        List<String> factory = List.of("KeyboardMouseOnly");

        assertTrue(Rename.factoryNamed("KeyboardMouseOnly.4.2.binds", factory));
        assertFalse(Rename.factoryNamed("Custom.4.2.binds", factory));
    }

    @Test
    void theNamesAreTheFactoryOnesAndTheSetsOwn() {
        ConfigurationFiles current = set(Map.of("Custom.4.2.binds", "<Root/>", "Custom.4.1.binds", "<Root/>",
                "Spare.4.2.binds", "<Root/>", "StartPreset.4.start", "Custom"));
        BindNameList list = new BindNameList(List.of(
                new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                        List.of(Path.of("Steam/ControlSchemes/KeyboardMouseOnly.binds"))),
                new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds")))), List.of());

        Rename.Names names = Rename.names(current, new Apply.Offered(list, List.of()));

        assertEquals(List.of("KeyboardMouseOnly"), names.factory());
        assertEquals(List.of("Custom", "Spare"), names.own());
    }

    @Test
    void theVersionAndExtensionAreFixed() {
        assertEquals(".4.2.binds", Rename.suffix("DualVirpilDawnTreader.4.2.binds"));
    }

    private static ConfigurationFiles set(Map<String, String> files) {
        Map<String, byte[]> bindings = new HashMap<>();
        files.forEach((name, content) -> bindings.put(name, bytes(content)));
        return new ConfigurationFiles(bindings, null, Map.of());
    }
}
