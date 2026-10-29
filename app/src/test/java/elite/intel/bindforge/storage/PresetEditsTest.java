package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.model.BindingSection;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The preset file as an edit (preset-editor.md, <em>What APPLY checks and writes for the preset file</em>; Storage S6):
 * the one file found, its four lines changed in the game's form, put back byte for byte, and checked.
 */
class PresetEditsTest {

    private static final String SPLIT = "KeyboardMouseOnly\r\nCustom\r\nCustom\r\nCustom";
    private static final StartPresetSections ALL_CUSTOM = new StartPresetSections("Custom", "Custom", "Custom", "Custom",
            false);
    private static final BindNameList OFFERED = new BindNameList(List.of(
            new BindNameEntry("Custom", BindsFileOrigin.USER, List.of(Path.of("Custom.4.2.binds"))),
            new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                    List.of(Path.of("Steam/ControlSchemes/KeyboardMouseOnly.binds")))), List.of());

    /** P3 review, finding 2: one file is the preset file, the highest N, whatever else the set holds. */
    @Test
    void thePresetFileIsTheHighestNumberedOne() {
        ConfigurationFiles files = set(Map.of("StartPreset.4.start", SPLIT, "StartPreset.10.start", "Custom",
                "StartPreset.3.start", "Old"));

        assertEquals(Optional.of("StartPreset.10.start"), PresetEdits.presetFile(files));
        assertEquals(ALL_CUSTOM, PresetEdits.sections(files).orElseThrow());
    }

    @Test
    void aSetWithNoPresetFileHasNoSections() {
        ConfigurationFiles files = set(Map.of());

        assertEquals(Optional.empty(), PresetEdits.presetFile(files));
        assertEquals(Optional.empty(), PresetEdits.sections(files));
    }

    /** Four lines, CRLF between them, no line ending after the last - the specimen's form. */
    @Test
    void aChangedLineIsWrittenInTheGamesForm() {
        ConfigurationFiles applied = set(Map.of("StartPreset.4.start", "﻿Custom\n"));

        ConfigurationFiles changed = PresetEdits.change(applied, applied,
                new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false));

        assertEquals(SPLIT, text(changed.bindings().get("StartPreset.4.start")));
    }

    /** Values are compared, never bytes: the single-line form already says all four. */
    @Test
    void theSameValuesLeaveTheSetAsItIs() {
        ConfigurationFiles applied = set(Map.of("StartPreset.4.start", "Custom\n"));

        ConfigurationFiles changed = PresetEdits.change(applied, applied, ALL_CUSTOM);

        assertSame(applied, changed);
    }

    /** Put back to what the working copy holds, the edits are the working copy again - so the bar goes dark. */
    @Test
    void linesPutBackPutBackTheWorkingCopysBytes() {
        ConfigurationFiles applied = set(Map.of("StartPreset.4.start", "Custom\n"));
        ConfigurationFiles edited = PresetEdits.change(applied, applied,
                new StartPresetSections("KeyboardMouseOnly", "Custom", "Custom", "Custom", false));

        ConfigurationFiles undone = PresetEdits.change(edited, applied, ALL_CUSTOM);

        assertTrue(undone.sameAs(applied));
    }

    @Test
    void aSetWithNoPresetFileGetsOneUnderTheLiveGamesName() {
        ConfigurationFiles none = set(Map.of());

        ConfigurationFiles changed = PresetEdits.change(none, none, ALL_CUSTOM);

        assertEquals("Custom\r\nCustom\r\nCustom\r\nCustom", text(changed.bindings().get("StartPreset.4.start")));
    }

    /** The one file found is the one changed - not a new StartPreset.4.start beside a StartPreset.10.start. */
    @Test
    void theFileChangedIsTheFileFound() {
        ConfigurationFiles applied = set(Map.of("StartPreset.4.start", SPLIT, "StartPreset.10.start", "Custom"));

        ConfigurationFiles changed = PresetEdits.change(applied, applied,
                new StartPresetSections("Custom", "KeyboardMouseOnly", "Custom", "Custom", false));

        assertEquals(SPLIT, text(changed.bindings().get("StartPreset.4.start")), "the older one is not touched");
        assertEquals("Custom\r\nKeyboardMouseOnly\r\nCustom\r\nCustom",
                text(changed.bindings().get("StartPreset.10.start")));
    }

    @Test
    void fourLinesTheListOffersPassTheCheck() {
        assertDoesNotThrow(() -> PresetEdits.check(set(Map.of("StartPreset.4.start", SPLIT)), OFFERED));
    }

    /** Exact match: the game's names are case-sensitive as far as anyone has shown. */
    @Test
    void aLineNamingNothingTheListOffersIsRefused() {
        PresetRefusedException refused = assertThrows(PresetRefusedException.class, () -> PresetEdits.check(
                set(Map.of("StartPreset.4.start", "Custom\r\ncustom\r\nCustom\r\nGone")), OFFERED));

        assertEquals(List.of(BindingSection.SHIP, BindingSection.ON_FOOT), refused.sections());
        assertTrue(refused.getMessage().contains("'Gone'"), refused.getMessage());
    }

    @Test
    void anEmptyLineIsRefused() {
        PresetRefusedException refused = assertThrows(PresetRefusedException.class, () -> PresetEdits.check(
                set(Map.of("StartPreset.4.start", "Custom\r\n\r\nCustom\r\nCustom")), OFFERED));

        assertEquals(List.of(BindingSection.SHIP), refused.sections());
    }

    @Test
    void noPresetFileIsRefusedForAllFour() {
        PresetRefusedException refused = assertThrows(PresetRefusedException.class,
                () -> PresetEdits.check(set(Map.of()), OFFERED));

        assertEquals(PresetEdits.SECTIONS, refused.sections());
    }

    @Test
    void theBindsFilesAreTheSetsBindsFilesAlone() {
        ConfigurationFiles files = set(Map.of("StartPreset.4.start", SPLIT, "Custom.4.2.binds", "<Root/>"));

        assertEquals(List.of("Custom.4.2.binds"), List.copyOf(PresetEdits.bindsFiles(files).keySet()));
    }

    private static ConfigurationFiles set(Map<String, String> files) {
        Map<String, byte[]> bindings = new HashMap<>();
        files.forEach((name, content) -> bindings.put(name, bytes(content)));
        return new ConfigurationFiles(bindings, null, Map.of());
    }
}
