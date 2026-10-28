package elite.intel.bindforge.io;

import elite.intel.bindforge.model.BindingModifier;
import elite.intel.bindforge.model.BindingSlotType;
import elite.intel.bindforge.rules.KeyboardKeyAvailabilityService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The writer's keyboard path and the keyboard availability it checks against. The writer edits a binds file's bytes in
 * memory (Storage S5); a temporary file stands in for the edits that hold them, and the file-stamp tests went with the
 * file the writer used to write.
 */
class BindingsWriterTest {
    @TempDir
    Path tempDir;

    private final KeyboardKeyAvailabilityService availabilityService = new KeyboardKeyAvailabilityService();
    private final KeyBindingsParser parser = KeyBindingsParser.getInstance();

    @Test
    void occupiedKeysAreCollectedFromEntireFileIncludingUnusedActions() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <UnusedByEliteIntel>
                        <Primary Device="Keyboard" Key="Key_L" />
                    </UnusedByEliteIntel>
                </Root>
                """);

        Set<String> occupied = availabilityService.occupiedKeyboardKeys(file);
        List<String> available = availabilityService.availableKeys(file, "GalaxyMapOpen", BindingSlotType.PRIMARY);

        assertTrue(occupied.contains("Key_L"));
        assertFalse(available.contains("Key_L"));
    }

    @Test
    void joystickHotasMouseAssignmentsDoNotOccupyKeyboardKeys() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <JoystickAction>
                        <Primary Device="044F0404" Key="Key_L" />
                        <Secondary Device="Mouse" Key="Key_M" />
                    </JoystickAction>
                </Root>
                """);

        Set<String> occupied = availabilityService.occupiedKeyboardKeys(file);
        List<String> available = availabilityService.availableKeys(file);

        assertFalse(occupied.contains("Key_L"));
        assertFalse(occupied.contains("Key_M"));
        assertTrue(available.contains("Key_L"));
        assertTrue(available.contains("Key_M"));
    }

    @Test
    void noDeviceBlankAndKeyPlaceholderDoNotOccupyKeys() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <EmptyAction>
                        <Primary Device="{NoDevice}" Key="Key_L" />
                        <Secondary Device="Keyboard" Key="" />
                    </EmptyAction>
                    <PlaceholderAction>
                        <Primary Device="Keyboard" Key="Key_" />
                        <Secondary Device="Keyboard" Key="{NoDevice}" />
                    </PlaceholderAction>
                </Root>
                """);

        Set<String> occupied = availabilityService.occupiedKeyboardKeys(file);

        assertFalse(occupied.contains("Key_L"));
        assertFalse(occupied.contains("Key_"));
        assertFalse(occupied.contains("{NoDevice}"));
    }

    @Test
    void assigningOccupiedKeyReturnsKeyOccupied() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <UnusedByEliteIntel>
                        <Primary Device="Keyboard" Key="Key_L" />
                    </UnusedByEliteIntel>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_L"));

        assertEquals(BindingSaveResult.KEY_OCCUPIED, result);
    }

    @Test
    void sameMainKeyWithDifferentModifierIsAvailableAndCanBeSaved() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <PlainAction>
                        <Primary Device="Keyboard" Key="Key_J" />
                    </PlainAction>
                    <ShiftAction>
                        <Primary Device="Keyboard" Key="Key_J">
                            <Modifier Device="Keyboard" Key="Key_RightShift" />
                        </Primary>
                    </ShiftAction>
                </Root>
                """);

        List<String> available = availabilityService.availableKeys(
                file,
                "GalaxyMapOpen",
                BindingSlotType.PRIMARY,
                new BindingModifier("Keyboard", "Key_LeftControl")
        );
        BindingSaveResult result = assignKeyboardKeyWithModifier(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_J"),
                new BindingModifier("Keyboard", "Key_LeftControl")
        );

        assertTrue(available.contains("Key_J"));
        assertEquals(BindingSaveResult.SAVED, result);
    }

    @Test
    void sameMainKeyWithSameModifierReturnsKeyOccupied() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <OtherAction>
                        <Primary Device="Keyboard" Key="Key_J">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                        </Primary>
                    </OtherAction>
                </Root>
                """);

        List<String> available = availabilityService.availableKeys(
                file,
                "GalaxyMapOpen",
                BindingSlotType.PRIMARY,
                new BindingModifier("Keyboard", "Key_LeftControl")
        );
        BindingSaveResult result = assignKeyboardKeyWithModifier(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_J"),
                new BindingModifier("Keyboard", "Key_LeftControl")
        );

        assertFalse(available.contains("Key_J"));
        assertEquals(BindingSaveResult.KEY_OCCUPIED, result);
    }

    @Test
    void sameMainKeyConflictsOnlyWhenTheFullModifierSetMatches() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <CtrlShiftAction>
                        <Primary Device="Keyboard" Key="Key_J">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                            <Modifier Device="Keyboard" Key="Key_LeftShift" />
                        </Primary>
                    </CtrlShiftAction>
                </Root>
                """);

        List<BindingModifier> ctrlShift = List.of(
                new BindingModifier("Keyboard", "Key_LeftControl"),
                new BindingModifier("Keyboard", "Key_LeftShift")
        );
        List<BindingModifier> ctrlOnly = List.of(new BindingModifier("Keyboard", "Key_LeftControl"));

        // Order within the set must not matter.
        List<BindingModifier> shiftCtrl = List.of(
                new BindingModifier("Keyboard", "Key_LeftShift"),
                new BindingModifier("Keyboard", "Key_LeftControl")
        );

        assertTrue(availabilityService.isKeyOccupiedByOtherSlot(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_J", shiftCtrl));
        assertFalse(availabilityService.isKeyOccupiedByOtherSlot(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_J", ctrlOnly));
        // The same answers for the file held in memory, as the edits hold it (Storage S5).
        byte[] binds = Files.readAllBytes(file);
        assertTrue(availabilityService.isKeyOccupiedByOtherSlot(binds, "GalaxyMapOpen", "Key_J", shiftCtrl));
        assertFalse(availabilityService.isKeyOccupiedByOtherSlot(binds, "GalaxyMapOpen", "Key_J", ctrlOnly));

        assertFalse(availabilityService.availableKeys(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, ctrlShift).contains("Key_J"));
        assertTrue(availabilityService.availableKeys(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, ctrlOnly).contains("Key_J"));
    }

    @Test
    void assigningUnknownKeyReturnsUnknownKey() throws Exception {
        Path file = writeBinds(minimalBinds());

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_NotReal"));

        assertEquals(BindingSaveResult.UNKNOWN_KEY, result);
    }

    @Test
    void writerChangesOnlySelectedSlotAndRemovesSupportedModifierForPlainSave() throws Exception {
        String original = """
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G">
                            <Modifier Device="Keyboard" Key="Key_LeftShift" />
                        </Primary>
                        <Secondary Device="045E028E" Key="Joy_1" />
                    </GalaxyMapOpen>
                    <UnusedByEliteIntel custom="kept">
                        <Primary Device="Keyboard" Key="Key_L" />
                        <UnknownChild value="preserved" />
                    </UnusedByEliteIntel>
                </Root>
                """;
        Path file = writeBinds(original);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Primary Device=\"Keyboard\" Key=\"Key_M\" />"));
        assertTrue(updated.contains("<Secondary Device=\"045E028E\" Key=\"Joy_1\" />"));
        assertTrue(updated.contains("<UnusedByEliteIntel custom=\"kept\">"));
        assertTrue(updated.contains("<UnknownChild value=\"preserved\" />"));
        assertFalse(updated.contains("<Modifier Device=\"Keyboard\" Key=\"Key_LeftShift\" />"));
    }

    @Test
    void crlfLineEndingsArePreservedOutsideSelectedSlot() throws Exception {
        String original = String.join("\r\n",
                "<Root>",
                "    <GalaxyMapOpen>",
                "        <Primary Device=\"{NoDevice}\" Key=\"\" />",
                "        <Secondary Device=\"045E028E\" Key=\"Joy_1\" />",
                "    </GalaxyMapOpen>",
                "    <Unrelated value=\"kept\" />",
                "</Root>",
                "");
        Path file = writeBinds(original);
        String expected = original.replace(
                "<Primary Device=\"{NoDevice}\" Key=\"\" />",
                "<Primary Device=\"Keyboard\" Key=\"Key_M\" />");

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.SAVED, result);
        assertEquals(expected, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void utf8BomIsPreservedOnWrite() throws Exception {
        Path file = tempDir.resolve("Test.4.1.binds");
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = minimalBinds().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(content, 0, withBom, bom.length, content.length);
        Files.write(file, withBom);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        byte[] updatedPrefix = new byte[3];
        System.arraycopy(Files.readAllBytes(file), 0, updatedPrefix, 0, 3);
        assertEquals(BindingSaveResult.SAVED, result);
        assertArrayEquals(bom, updatedPrefix);
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("Key=\"Key_M\""));
    }

    @Test
    void selectedNonKeyboardSlotCannotBeReplaced() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="045E028E" Key="Joy_1" />
                    </GalaxyMapOpen>
                </Root>
                """);
        String before = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(before, updated);
    }

    @Test
    void clearingKeyboardSlotWritesNoDeviceBlankAndPreservesOtherSlot() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="Keyboard" Key="Key_H" />
                    </GalaxyMapOpen>
                    <Unrelated value="kept" />
                </Root>
                """);
        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Primary Device=\"{NoDevice}\" Key=\"\" />"));
        assertTrue(updated.contains("<Secondary Device=\"Keyboard\" Key=\"Key_H\" />"));
        assertTrue(updated.contains("<Unrelated value=\"kept\" />"));
    }

    @Test
    void clearingSupportedKeyboardModifierSlotWritesNoDeviceBlank() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_J">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                        </Primary>
                        <Secondary Device="Keyboard" Key="Key_H" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Primary Device=\"{NoDevice}\" Key=\"\" />"));
        assertTrue(updated.contains("<Secondary Device=\"Keyboard\" Key=\"Key_H\" />"));
        assertFalse(updated.contains("<Modifier Device=\"Keyboard\" Key=\"Key_LeftControl\" />"));
    }

    @Test
    void clearingNonKeyboardSlotIsUnsupportedAndPreservesFile() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="045E028E" Key="Joy_1" />
                    </GalaxyMapOpen>
                </Root>
                """);
        String before = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(before, updated);
    }

    @Test
    void parserMarksKeyboardKeyWithoutModifierEditable() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        KeyBindingsParser.ReadOnlyBindingSlot slot = parser.parseReadOnlyBindingSlots(file.toFile())
                .get("GalaxyMapOpen")
                .primary();

        assertTrue(slot.editable());
        assertTrue(slot.keyboardUsable());
        assertEquals(0, slot.bindingModifiers().size());
    }

    @Test
    void parserMarksKeyboardKeyWithOneKeyboardModifierEditable() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        KeyBindingsParser.ReadOnlyBindingSlot slot = parser.parseReadOnlyBindingSlots(file.toFile())
                .get("GalaxyMapOpen")
                .primary();

        assertTrue(slot.editable());
        assertTrue(slot.keyboardUsable());
        assertEquals(List.of(new BindingModifier("Keyboard", "Key_LeftControl")), slot.bindingModifiers());
        assertArrayEquals(new String[]{"Key_LeftControl"}, slot.modifiers());
    }

    @Test
    void parserMarksKeyboardKeyWithMultipleKeyboardModifiersEditable() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                            <Modifier Device="Keyboard" Key="Key_LeftAlt" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        KeyBindingsParser.ReadOnlyBindingSlot slot = parser.parseReadOnlyBindingSlots(file.toFile())
                .get("GalaxyMapOpen")
                .primary();

        assertTrue(slot.editable());
        assertTrue(slot.keyboardUsable());
        assertEquals(List.of(
                new BindingModifier("Keyboard", "Key_LeftControl"),
                new BindingModifier("Keyboard", "Key_LeftAlt")
        ), slot.bindingModifiers());
        assertArrayEquals(new String[]{"Key_LeftControl", "Key_LeftAlt"}, slot.modifiers());
    }

    @Test
    void parserPreservesMouseModifierAndMarksSlotUnsupported() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Mouse" Key="Mouse_1" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        KeyBindingsParser.ReadOnlyBindingSlot slot = parser.parseReadOnlyBindingSlots(file.toFile())
                .get("GalaxyMapOpen")
                .primary();

        assertFalse(slot.editable());
        assertFalse(slot.keyboardUsable());
        assertEquals(List.of(new BindingModifier("Mouse", "Mouse_1")), slot.bindingModifiers());
    }

    @Test
    void writerSavesOneKeyboardModifierAsValidBindsXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKeyWithModifier(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_I"),
                new BindingModifier("Keyboard", "Key_LeftControl"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("""
                <Primary Device="Keyboard" Key="Key_I">
                    <Modifier Device="Keyboard" Key="Key_LeftControl" />
                </Primary>
                """.stripTrailing()));
    }

    @Test
    void writerDoesNotRemoveUnsupportedModifierNodesWhenSavingAnotherSlot() throws Exception {
        String original = """
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Mouse" Key="Mouse_1" />
                        </Primary>
                        <Secondary Device="Keyboard" Key="Key_H" />
                    </GalaxyMapOpen>
                </Root>
                """;
        Path file = writeBinds(original);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Modifier Device=\"Mouse\" Key=\"Mouse_1\" />"));
        assertTrue(updated.contains("<Secondary Device=\"Keyboard\" Key=\"Key_M\" />"));
    }

    @Test
    void writerSavesTwoKeyboardModifiersAsValidBindsXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKeyWithModifiers(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_I"),
                List.of(
                        new BindingModifier("Keyboard", "Key_LeftControl"),
                        new BindingModifier("Keyboard", "Key_LeftShift")
                ));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("""
                <Primary Device="Keyboard" Key="Key_I">
                    <Modifier Device="Keyboard" Key="Key_LeftControl" />
                    <Modifier Device="Keyboard" Key="Key_LeftShift" />
                </Primary>
                """.stripTrailing()));
    }

    @Test
    void plainSaveOverMultiModifierSlotRemovesAllSupportedModifiers() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                            <Modifier Device="Keyboard" Key="Key_LeftAlt" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Primary Device=\"Keyboard\" Key=\"Key_M\" />"));
        assertFalse(updated.contains("<Modifier"));
    }

    @Test
    void writerRejectsMixedSupportedAndUnsupportedModifierRequest() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKeyWithModifiers(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_I"),
                List.of(
                        new BindingModifier("Keyboard", "Key_LeftControl"),
                        new BindingModifier("Mouse", "Mouse_1")
                ));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void writerRejectsMouseModifierAndPreservesItUnchanged() throws Exception {
        String original = """
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_I">
                            <Modifier Device="Mouse" Key="Mouse_1" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """;
        Path file = writeBinds(original);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void clearingAlreadyNoDeviceBlankSlotIsNoChange() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="Keyboard" Key="Key_H" />
                    </GalaxyMapOpen>
                </Root>
                """);
        String before = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, null));

        assertEquals(BindingSaveResult.NO_CHANGE, result);
        assertEquals(before, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void noChangeLeavesTheFileAsItWas() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_M" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);
        String before = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.NO_CHANGE, result);
        assertEquals(before, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void missingBindingReturnsBindingNotFound() throws Exception {
        Path file = writeBinds(minimalBinds());

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "DoesNotExist", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, result);
    }

    @Test
    void duplicateActionNodeReturnsUnsupportedXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void selectedSlotNotFoundReturnsUnsupportedXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void ambiguousUnsupportedXmlReturnsUnsupportedXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void unknownSelectedSlotAttributeReturnsUnsupportedXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" Mystery="keep-me" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void unknownSelectedSlotChildReturnsUnsupportedXml() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="">
                            <Unexpected value="keep-me" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
    }

    @Test
    void assigningOverAHoldSlotKeepsTheHoldChild() throws Exception {
        // Press-and-hold bindings (e.g. on-foot weapon switch) carry a <Hold Value="1" /> child.
        // Reassigning the key must not silently turn the binding into a tap: the Hold child survives.
        Path file = writeBinds("""
                <Root>
                    <HumanoidSwitchWeapon>
                        <Primary Device="Keyboard" Key="Key_1">
                            <Hold Value="1" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </HumanoidSwitchWeapon>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "HumanoidSwitchWeapon", BindingSlotType.PRIMARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("""
                <Primary Device="Keyboard" Key="Key_M">
                    <Hold Value="1" />
                </Primary>
                """.stripTrailing()));
    }

    @Test
    void assigningWithModifierOverAHoldSlotKeepsHoldAndAddsModifier() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <OrderHoldFire>
                        <Primary Device="Keyboard" Key="Key_F3">
                            <Hold Value="1" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </OrderHoldFire>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKeyWithModifier(
                edit(file, "OrderHoldFire", BindingSlotType.PRIMARY, "Key_M"),
                new BindingModifier("Keyboard", "Key_LeftControl"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("""
                <Primary Device="Keyboard" Key="Key_M">
                    <Hold Value="1" />
                    <Modifier Device="Keyboard" Key="Key_LeftControl" />
                </Primary>
                """.stripTrailing()));
    }

    @Test
    void plainSaveOverAHoldAndModifierSlotKeepsHoldAndDropsModifier() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <OrderHoldFire>
                        <Primary Device="Keyboard" Key="Key_F3">
                            <Hold Value="1" />
                            <Modifier Device="Keyboard" Key="Key_LeftControl" />
                        </Primary>
                        <Secondary Device="{NoDevice}" Key="" />
                    </OrderHoldFire>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "OrderHoldFire", BindingSlotType.PRIMARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Hold Value=\"1\" />"));
        assertFalse(updated.contains("<Modifier"));
    }

    @Test
    void clearingAHoldSlotWritesNoDeviceBlankAndDropsHold() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <EjectAllCargo>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="Keyboard" Key="Key_Delete">
                            <Hold Value="1" />
                        </Secondary>
                    </EjectAllCargo>
                </Root>
                """);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "EjectAllCargo", BindingSlotType.SECONDARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Secondary Device=\"{NoDevice}\" Key=\"\" />"));
        assertFalse(updated.contains("<Hold"));
    }

    @Test
    void aControllerSlotIsNeverEdited() throws Exception {
        // The commander bound this device in the game. No caller, and no flag, may take it.
        Path file = writeBinds(controllerOnlyBinds());
        String original = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult secondary = assignKeyboardKeyWithModifier(
                edit(file, "UpThrustButton", BindingSlotType.SECONDARY, "Key_E"),
                new BindingModifier("Keyboard", "Key_LeftControl"));
        BindingSaveResult primary = assignKeyboardKey(
                edit(file, "UpThrustButton", BindingSlotType.PRIMARY, "Key_E"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, secondary);
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, primary);
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8),
                "a refused edit must leave the controller bindings byte-for-byte intact");
    }

    @Test
    void clearingAControllerSlotIsAlsoRefused() throws Exception {
        Path file = writeBinds(controllerOnlyBinds());
        String original = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = assignKeyboardKey(
                edit(file, "UpThrustButton", BindingSlotType.SECONDARY, null));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    private String controllerOnlyBinds() {
        return """
                <Root>
                    <UpThrustButton>
                        <Primary Device="T16000MTHROTTLE" DeviceIndex="0" Key="Joy_4" />
                        <Secondary Device="T16000MTHROTTLE" DeviceIndex="2" Key="Joy_7">
                            <Modifier Device="T16000MTHROTTLE" DeviceIndex="1" Key="Joy_7" />
                        </Secondary>
                    </UpThrustButton>
                </Root>
                """;
    }

    /** An edit, and the file standing in for the edits that hold the binds file. */
    private record OnFile(Path file, KeyboardBindingEdit edit) {
    }

    private OnFile edit(Path file, String bindingId, BindingSlotType slotType, String key) {
        return new OnFile(file, new KeyboardBindingEdit(bindingId, slotType, key));
    }

    private BindingSaveResult assignKeyboardKey(OnFile edit) throws IOException {
        return kept(edit.file(), new BindingsWriter().assignKeyboardKey(Files.readAllBytes(edit.file()), edit.edit()));
    }

    private BindingSaveResult assignKeyboardKeyWithModifier(OnFile edit, BindingModifier modifier) throws IOException {
        return kept(edit.file(), new BindingsWriter().assignKeyboardKeyWithModifier(
                Files.readAllBytes(edit.file()), edit.edit(), modifier));
    }

    private BindingSaveResult assignKeyboardKeyWithModifiers(OnFile edit, List<BindingModifier> modifiers)
            throws IOException {
        return kept(edit.file(), new BindingsWriter().assignKeyboardKeyWithModifiers(
                Files.readAllBytes(edit.file()), edit.edit(), modifiers));
    }

    /** The edit's bytes put back in the file when they changed; otherwise the writer must hand back what it was given. */
    private static BindingSaveResult kept(Path file, EditedBinds edited) throws IOException {
        if (edited.changed()) {
            Files.write(file, edited.content());
        } else {
            assertArrayEquals(Files.readAllBytes(file), edited.content(), "an edit not made changes nothing");
        }
        return edited.result();
    }

    private Path writeBinds(String xml) throws IOException {
        Path file = tempDir.resolve("Test.4.1.binds");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file;
    }

    private String minimalBinds() {
        return """
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="{NoDevice}" Key="" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """;
    }
}
