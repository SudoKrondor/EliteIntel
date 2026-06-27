package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_L"));

        assertEquals(BindingSaveResult.KEY_OCCUPIED, result);
        assertEquals(0, backups(file).size());
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
        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifier(
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
        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifier(
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

        assertFalse(availabilityService.availableKeys(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, ctrlShift).contains("Key_J"));
        assertTrue(availabilityService.availableKeys(
                file, "GalaxyMapOpen", BindingSlotType.PRIMARY, ctrlOnly).contains("Key_J"));
    }

    @Test
    void assigningUnknownKeyReturnsUnknownKey() throws Exception {
        Path file = writeBinds(minimalBinds());

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_NotReal"));

        assertEquals(BindingSaveResult.UNKNOWN_KEY, result);
        assertEquals(0, backups(file).size());
    }

    @Test
    void staleTimestampOrSizeReturnsStaleFile() throws Exception {
        Path file = writeBinds(minimalBinds());
        KeyboardBindingEdit staleEdit = edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M");
        Files.writeString(file, minimalBinds() + "\n<!-- changed by game -->", StandardCharsets.UTF_8);

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(staleEdit);

        assertEquals(BindingSaveResult.STALE_FILE, result);
        assertEquals(0, backups(file).size());
    }

    @Test
    void staleSizeMismatchReturnsStaleFile() throws Exception {
        Path file = writeBinds(minimalBinds());
        KeyboardBindingEdit edit = new KeyboardBindingEdit(
                file,
                "GalaxyMapOpen",
                BindingSlotType.PRIMARY,
                "Key_M",
                Files.getLastModifiedTime(file),
                Files.size(file) + 1
        );

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(edit);

        assertEquals(BindingSaveResult.STALE_FILE, result);
        assertEquals(0, backups(file).size());
    }

    @Test
    void staleTimestampMismatchReturnsStaleFile() throws Exception {
        Path file = writeBinds(minimalBinds());
        KeyboardBindingEdit edit = edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M");
        Files.setLastModifiedTime(file, FileTime.fromMillis(edit.expectedLastModified().toMillis() + 5_000));

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(edit);

        assertEquals(BindingSaveResult.STALE_FILE, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_M"));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(before, updated);
        assertEquals(0, backups(file).size());
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
        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(before, updated);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifier(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifiers(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifiers(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, null));

        assertEquals(BindingSaveResult.NO_CHANGE, result);
        assertEquals(before, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(0, backups(file).size());
    }

    @Test
    void staleClearReturnsStaleFileWithoutBackupOrWrite() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_G" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);
        KeyboardBindingEdit staleEdit = edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, null);
        Files.writeString(file, Files.readString(file, StandardCharsets.UTF_8) + "\n<!-- changed by game -->", StandardCharsets.UTF_8);

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(staleEdit);

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.STALE_FILE, result);
        assertTrue(updated.contains("<Primary Device=\"Keyboard\" Key=\"Key_G\" />"));
        assertTrue(updated.contains("changed by game"));
        assertEquals(0, backups(file).size());
    }

    @Test
    void noChangeDoesNotCreateBackupOrWrite() throws Exception {
        Path file = writeBinds("""
                <Root>
                    <GalaxyMapOpen>
                        <Primary Device="Keyboard" Key="Key_M" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </GalaxyMapOpen>
                </Root>
                """);
        String before = Files.readString(file, StandardCharsets.UTF_8);

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.NO_CHANGE, result);
        assertEquals(before, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(0, backups(file).size());
    }

    @Test
    void missingBindingReturnsBindingNotFound() throws Exception {
        Path file = writeBinds(minimalBinds());

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "DoesNotExist", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.BINDING_NOT_FOUND, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.SECONDARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "GalaxyMapOpen", BindingSlotType.PRIMARY, "Key_M"));

        assertEquals(BindingSaveResult.UNSUPPORTED_XML, result);
        assertEquals(0, backups(file).size());
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKeyWithModifier(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
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

        BindingSaveResult result = new BindingsWriter().assignKeyboardKey(
                edit(file, "EjectAllCargo", BindingSlotType.SECONDARY, null));

        String updated = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(BindingSaveResult.SAVED, result);
        assertTrue(updated.contains("<Secondary Device=\"{NoDevice}\" Key=\"\" />"));
        assertFalse(updated.contains("<Hold"));
    }

    private KeyboardBindingEdit edit(
            Path file,
            String bindingId,
            BindingSlotType slotType,
            String key
    ) throws IOException {
        return new KeyboardBindingEdit(
                file,
                bindingId,
                slotType,
                key,
                Files.getLastModifiedTime(file),
                Files.size(file)
        );
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

    private List<Path> backups(Path bindsFile) throws IOException {
        try (var files = Files.list(bindsFile.getParent())) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".bak"))
                    .toList();
        }
    }

}
