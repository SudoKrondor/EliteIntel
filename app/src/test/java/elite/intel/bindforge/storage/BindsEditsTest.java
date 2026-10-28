package elite.intel.bindforge.storage;

import elite.intel.bindforge.io.BindingSaveResult;
import elite.intel.bindforge.io.BindingsWriter;
import elite.intel.bindforge.io.KeyboardBindingEdit;
import elite.intel.bindforge.io.SlotAssignment;
import elite.intel.bindforge.model.BindingInput;
import elite.intel.bindforge.model.BindingSlotType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Bind Editor's changes to the edits not applied (Storage S5): which binds files it offers, which it opens first,
 * and a SAVE made all or none.
 */
class BindsEditsTest {

    private static final String BINDS = """
            <Root PresetName="Custom" MajorVersion="4" MinorVersion="2">
            \t<UpThrustButton>
            \t\t<Primary Device="{NoDevice}" Key="" />
            \t\t<Secondary Device="{NoDevice}" Key="" />
            \t</UpThrustButton>
            \t<OddButton>
            \t\t<Primary Device="Throttle" Key="Joy_2">
            \t\t\t<Unknown Value="1" />
            \t\t</Primary>
            \t\t<Secondary Device="{NoDevice}" Key="" />
            \t</OddButton>
            </Root>
            """;

    private final BindingsWriter writer = new BindingsWriter();

    @Test
    void everyBindsFileIsOfferedAndThePresetFileIsNot() {
        ConfigurationFiles files = set(Map.of(
                "Testing.4.2.binds", BINDS,
                "Custom.4.2.binds", BINDS,
                "StartPreset.4.start", "Custom\r\nCustom\r\nCustom\r\nCustom"));

        assertEquals(List.of("Custom.4.2.binds", "Testing.4.2.binds"), BindsEdits.bindsFiles(files));
    }

    @Test
    void theGeneralLinesFileOpensFirstInItsNewestVersion() {
        ConfigurationFiles files = set(Map.of(
                "Alpha.4.2.binds", BINDS,
                "Custom.4.1.binds", BINDS,
                "Custom.4.10.binds", BINDS,
                "StartPreset.4.start", "﻿Custom\r\nAlpha\r\nAlpha\r\nAlpha"));

        assertEquals(Optional.of("Custom.4.10.binds"), BindsEdits.generalFile(files), "4.10 is newer than 4.1");
    }

    @Test
    void noPresetFileABlankGeneralLineOrNoSuchFileOpensNothingInParticular() {
        assertEquals(Optional.empty(), BindsEdits.generalFile(set(Map.of("Custom.4.2.binds", BINDS))));
        assertEquals(Optional.empty(), BindsEdits.generalFile(set(Map.of(
                "Custom.4.2.binds", BINDS, "StartPreset.4.start", "\r\nCustom\r\nCustom\r\nCustom"))));
        assertEquals(Optional.empty(), BindsEdits.generalFile(set(Map.of(
                "Custom.4.2.binds", BINDS, "StartPreset.4.start", "KeyboardMouseOnly"))));
    }

    @Test
    void aSaveChangesOnlyItsBindsFile() {
        ConfigurationFiles files = set(Map.of("Custom.4.2.binds", BINDS, "Testing.4.2.binds", BINDS));

        BindsEdits.Changed changed = BindsEdits.change(files, "Testing.4.2.binds",
                binds -> writer.assignKeyboardKey(binds, key("UpThrustButton", BindingSlotType.PRIMARY, "Key_R")));

        assertEquals(BindingSaveResult.SAVED, changed.result());
        assertTrue(text(changed.files(), "Testing.4.2.binds").contains("<Primary Device=\"Keyboard\" Key=\"Key_R\" />"));
        assertEquals(BINDS, text(changed.files(), "Custom.4.2.binds"));
        assertEquals(BINDS, text(files, "Testing.4.2.binds"), "the set handed in is not changed");
    }

    @Test
    void bothSlotsOfOneSaveAreMadeTogether() {
        ConfigurationFiles files = set(Map.of("Custom.4.2.binds", BINDS));

        BindsEdits.Changed changed = BindsEdits.change(files, "Custom.4.2.binds", List.of(
                binds -> writer.assignKeyboardKey(binds, key("UpThrustButton", BindingSlotType.PRIMARY, "Key_R")),
                binds -> writer.assignSlot(binds, new SlotAssignment("UpThrustButton", BindingSlotType.SECONDARY,
                        new BindingInput("Throttle", "Joy_6"), List.of(), false))));

        assertEquals(BindingSaveResult.SAVED, changed.result());
        String text = text(changed.files(), "Custom.4.2.binds");
        assertTrue(text.contains("<Primary Device=\"Keyboard\" Key=\"Key_R\" />"));
        assertTrue(text.contains("<Secondary Device=\"Throttle\" Key=\"Joy_6\" />"));
    }

    /** One refused slot leaves the whole SAVE undone, so no half-changed row is left on screen. */
    @Test
    void aRefusedSlotLeavesTheSetAsItWas() {
        ConfigurationFiles files = set(Map.of("Custom.4.2.binds", BINDS));

        BindsEdits.Changed changed = BindsEdits.change(files, "Custom.4.2.binds", List.of(
                binds -> writer.assignKeyboardKey(binds, key("UpThrustButton", BindingSlotType.PRIMARY, "Key_R")),
                binds -> writer.assignSlot(binds, new SlotAssignment("OddButton", BindingSlotType.PRIMARY,
                        new BindingInput("Throttle", "Joy_3"), List.of(), false))));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, changed.result());
        assertSame(files, changed.files());
    }

    @Test
    void nothingToChangeIsNoChange() {
        ConfigurationFiles files = set(Map.of("Custom.4.2.binds", BINDS));

        BindsEdits.Changed changed = BindsEdits.change(files, "Custom.4.2.binds",
                binds -> writer.assignKeyboardKey(binds, key("UpThrustButton", BindingSlotType.PRIMARY, null)));

        assertEquals(BindingSaveResult.NO_CHANGE, changed.result());
        assertSame(files, changed.files());
    }

    @Test
    void aFileTheSetDoesNotHoldIsRefused() {
        ConfigurationFiles files = set(Map.of("Custom.4.2.binds", BINDS, "StartPreset.4.start", "Custom"));

        assertThrows(IllegalArgumentException.class, () -> BindsEdits.change(files, "Gone.4.2.binds", List.of()));
        assertThrows(IllegalArgumentException.class, () -> BindsEdits.change(files, "StartPreset.4.start", List.of()),
                "the preset file is not a binds file");
    }

    @Test
    void aBindNameIsTheFileNameBeforeItsVersion() {
        assertEquals(Optional.of("Dual.Virpil"), BindsEdits.bindNameOf("Dual.Virpil.4.2.binds"));
        assertEquals(Optional.empty(), BindsEdits.bindNameOf("StartPreset.4.start"));
    }

    private static KeyboardBindingEdit key(String bindingId, BindingSlotType slot, String key) {
        return new KeyboardBindingEdit(bindingId, slot, key);
    }

    private static ConfigurationFiles set(Map<String, String> bindings) {
        Map<String, byte[]> bytes = new TreeMap<>();
        bindings.forEach((name, text) -> bytes.put(name, text.getBytes(StandardCharsets.UTF_8)));
        return new ConfigurationFiles(bytes, null, Map.of());
    }

    private static String text(ConfigurationFiles files, String name) {
        return new String(files.bindings().get(name), StandardCharsets.UTF_8);
    }
}
