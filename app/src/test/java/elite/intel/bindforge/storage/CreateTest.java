package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.StartPresetSections;
import elite.intel.bindforge.preseteditor.BindNameCatalog;
import elite.intel.bindforge.preseteditor.BindNameEntry;
import elite.intel.bindforge.preseteditor.BindNameList;
import elite.intel.bindforge.preseteditor.BindsFileOrigin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CREATE (storage.md rule 16; Storage S11): when it shows, what it offers, and the edit it makes - the factory file
 * under the new name and its version, its {@code PresetName} changed and nothing else, and all four preset lines.
 */
class CreateTest {

    private static final String FACTORY = "﻿<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
            + "<Root PresetName=\"KeyboardMouseOnly\" MajorVersion=\"4\" MinorVersion=\"2\">\r\n"
            + "\t<!-- Frontier's comment, accents kept: café -->\r\n"
            + "\t<KeyboardLayout>en-US</KeyboardLayout>\r\n"
            + "\t<YawLeftButton>\r\n"
            + "\t\t<Primary Device=\"Keyboard\" Key=\"Key_A\" />\r\n"
            + "\t\t<Secondary Device=\"{NoDevice}\" Key=\"\" />\r\n"
            + "\t</YawLeftButton>\r\n"
            + "</Root>\r\n";

    private static final String NEW_PLAYER_PRESET = "KeyboardMouseOnly\r\nKeyboardMouseOnly\r\nKeyboardMouseOnly\r\n"
            + "KeyboardMouseOnly";

    @Test
    void createIsOfferedWhileTheWorkingCopyHoldsNoBindsFile() {
        ConfigurationFiles newPlayer = set(Map.of("StartPreset.4.start", NEW_PLAYER_PRESET));

        assertEquals(new Create.State(true, false), Create.State.of(new Edits.Snapshot(newPlayer, newPlayer)));
    }

    /** After CREATE the tabs come alive and CREATE stays, dimmed; after APPLY it is gone. */
    @Test
    void afterCreateTheEditorsOpenAndAfterApplyCreateGoes() throws IOException {
        ConfigurationFiles newPlayer = set(Map.of("StartPreset.4.start", NEW_PLAYER_PRESET));
        ConfigurationFiles created = Create.create(newPlayer, newPlayer, "Custom", bytes(FACTORY));

        assertEquals(new Create.State(true, true), Create.State.of(new Edits.Snapshot(created, newPlayer)));
        assertEquals(new Create.State(false, true), Create.State.of(new Edits.Snapshot(created, created)));
    }

    @Test
    void theFileIsNamedWithTheVersionOnItsRoot() throws IOException {
        String older = FACTORY.replace("MinorVersion=\"2\"", "MinorVersion=\"1\"");

        assertEquals("Custom.4.1.binds", Create.fileName("Custom", bytes(older)));
    }

    /** Alan, 2026-11-02, Q1: with no version on the root, the Live game's. */
    @Test
    void aRootWithNoVersionGetsTheLiveGames() throws IOException {
        String bare = FACTORY.replace(" MajorVersion=\"4\" MinorVersion=\"2\"", "");

        assertEquals("Custom.4.2.binds", Create.fileName("Custom", bytes(bare)));
    }

    @Test
    void aVersionThatIsNotANumberGetsTheLiveGames() throws IOException {
        String odd = FACTORY.replace("MajorVersion=\"4\"", "MajorVersion=\"four\"");

        assertEquals("Custom.4.2.binds", Create.fileName("Custom", bytes(odd)));
    }

    /** The PresetName must match the file name, or the file is not offered; nothing else is touched. */
    @Test
    void onlyThePresetNameChangesEveryOtherByteKept() throws IOException {
        byte[] factory = bytes(FACTORY);

        byte[] created = Create.withPresetName(factory, "DawnTreaderKMO");

        byte[] expected = bytes(FACTORY.replace("PresetName=\"KeyboardMouseOnly\"", "PresetName=\"DawnTreaderKMO\""));
        assertArrayEquals(expected, created);
    }

    @Test
    void aRootWithNoPresetNameGetsOne() throws IOException {
        String unnamed = FACTORY.replace("PresetName=\"KeyboardMouseOnly\" ", "");

        String created = text(Create.withPresetName(bytes(unnamed), "Custom"));

        assertTrue(created.contains("<Root PresetName=\"Custom\" MajorVersion=\"4\" MinorVersion=\"2\">"), created);
    }

    @Test
    void aFileWithNoRootIsRefused() {
        assertThrows(IOException.class, () -> Create.withPresetName(bytes("<Other />"), "Custom"));
    }

    @Test
    void createAddsTheFileAndPointsAllFourLinesAtIt() throws IOException {
        ConfigurationFiles newPlayer = set(Map.of("StartPreset.4.start", NEW_PLAYER_PRESET));

        ConfigurationFiles created = Create.create(newPlayer, newPlayer, "Custom", bytes(FACTORY));

        assertEquals(new StartPresetSections("Custom", "Custom", "Custom", "Custom", false),
                PresetEdits.sections(created).orElseThrow());
        assertEquals(List.of("Custom.4.2.binds"), BindsEdits.bindsFiles(created));
        // WHY: what APPLY's preset check offers - the new file must be one a line can name.
        BindNameList offered = BindNameCatalog.list(PresetEdits.bindsFiles(created), List.of());
        assertEquals(BindsFileOrigin.USER, offered.find("Custom").orElseThrow().origin());
        assertEquals(List.of(), offered.notOffered());
    }

    @Test
    void aSetWithNoPresetFileGetsOne() throws IOException {
        ConfigurationFiles empty = set(Map.of());

        ConfigurationFiles created = Create.create(empty, empty, "Custom", bytes(FACTORY));

        assertEquals("Custom\r\nCustom\r\nCustom\r\nCustom",
                text(created.bindings().get(PresetEdits.NEW_PRESET_FILE)));
    }

    /** DISCARD after CREATE is the working copy again: the edits leave the working copy untouched. */
    @Test
    void createLeavesTheWorkingCopyAsItWas() throws IOException {
        ConfigurationFiles newPlayer = set(Map.of("StartPreset.4.start", NEW_PLAYER_PRESET));

        Create.create(newPlayer, newPlayer, "Custom", bytes(FACTORY));

        assertEquals(NEW_PLAYER_PRESET, text(newPlayer.bindings().get("StartPreset.4.start")));
        assertFalse(Create.hasBindsFile(newPlayer));
    }

    @Test
    void createIsRefusedOnceTheEditsHoldABindsFile() {
        ConfigurationFiles existing = set(Map.of("Custom.4.2.binds", FACTORY));

        assertThrows(IllegalStateException.class,
                () -> Create.create(existing, existing, "Other", bytes(FACTORY)));
    }

    @Test
    void aNameThatIsNotAFileNameIsRefused() {
        ConfigurationFiles empty = set(Map.of());

        assertThrows(IllegalArgumentException.class, () -> Create.create(empty, empty, "../Custom", bytes(FACTORY)));
    }

    /** KeyboardMouseOnly first, the rest by name ignoring case; a factory file in two installations is one choice. */
    @Test
    void theChoicesPutKeyboardMouseOnlyFirst() {
        Path steamKmo = Path.of("Steam/ControlSchemes/KeyboardMouseOnly.binds");
        BindNameList list = new BindNameList(List.of(
                factory("GamePadDefault"),
                factory("keyboardOnly"),
                new BindNameEntry("KeyboardMouseOnly", BindsFileOrigin.FACTORY,
                        List.of(steamKmo, Path.of("Epic/ControlSchemes/KeyboardMouseOnly.binds"))),
                new BindNameEntry("Spare", BindsFileOrigin.USER, List.of(Path.of("Spare.4.2.binds"))),
                factory("SaitekX56")), List.of());

        Create.Choices choices = Create.choices(new Apply.Offered(list, List.of()));

        assertEquals(List.of("KeyboardMouseOnly", "GamePadDefault", "keyboardOnly", "SaitekX56"),
                choices.factories().stream().map(Create.Factory::bindName).toList());
        assertEquals(steamKmo, choices.factories().getFirst().file());
        assertTrue(choices.taken().containsAll(
                Arrays.asList("GamePadDefault", "keyboardOnly", "KeyboardMouseOnly", "Spare", "SaitekX56")));
    }

    private static BindNameEntry factory(String bindName) {
        return new BindNameEntry(bindName, BindsFileOrigin.FACTORY,
                List.of(Path.of("Steam/ControlSchemes/" + bindName + ".binds")));
    }

    private static ConfigurationFiles set(Map<String, String> files) {
        Map<String, byte[]> bindings = new HashMap<>();
        files.forEach((name, content) -> bindings.put(name, bytes(content)));
        return new ConfigurationFiles(bindings, null, Map.of());
    }
}
