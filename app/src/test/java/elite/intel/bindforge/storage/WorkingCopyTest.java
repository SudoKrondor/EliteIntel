package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The working copy as files: laid out like the game's, read whole, written whole in one step, and a write that
 * stopped part way finished or thrown away - never half one set and half another (storage.md, <em>How the files
 * are written</em>).
 */
class WorkingCopyTest {

    private static final byte[] BOM_BINDS = bytes("﻿<Root PresetName=\"Custom\"/>\r\n");

    @TempDir
    Path root;

    @Test
    void aSetIsWrittenLaidOutLikeTheGameAndReadBackByteForByte() throws IOException {
        WorkingCopy workingCopy = new WorkingCopy(root);

        workingCopy.write(set(BOM_BINDS, "<Mappings/>", "<LVWAP/>"));

        assertArrayEquals(BOM_BINDS, Files.readAllBytes(root.resolve("Bindings/Custom.4.2.binds")),
                "the byte-order mark and line endings are kept");
        assertEquals("Custom\r\nCustom\r\nCustom\r\nCustom", Files.readString(root.resolve("Bindings/StartPreset.4.start")));
        assertEquals("<Mappings/>", Files.readString(root.resolve("ControlSchemes/DeviceMappings.xml")));
        assertEquals("<LVWAP/>", Files.readString(root.resolve("ControlSchemes/DeviceButtonMaps/LVWAP.buttonMap")));

        ConfigurationFiles read = workingCopy.read();
        assertArrayEquals(BOM_BINDS, read.bindings().get("Custom.4.2.binds"));
        assertEquals(2, read.bindings().size());
        assertEquals("<Mappings/>", text(read.deviceMappings()));
        assertEquals("<LVWAP/>", text(read.buttonMaps().get("LVWAP.buttonMap")));
    }

    @Test
    void aWriteReplacesTheWholeSetSoAFileTheNewSetLacksIsGone() throws IOException {
        WorkingCopy workingCopy = new WorkingCopy(root);
        workingCopy.write(set(BOM_BINDS, "<Mappings/>", "<LVWAP/>"));

        workingCopy.write(new ConfigurationFiles(Map.of("Testing.4.2.binds", bytes("<Testing/>")), null, Map.of()));

        ConfigurationFiles read = workingCopy.read();
        assertEquals(Map.of("Testing.4.2.binds", "<Testing/>"), texts(read.bindings()));
        assertFalse(read.hasDeviceMappings());
        assertTrue(read.buttonMaps().isEmpty());
        assertFalse(Files.exists(root.resolve("ControlSchemes/DeviceButtonMaps/LVWAP.buttonMap")));
        assertFalse(Files.exists(root.resolve(WorkingCopy.NEXT)), "nothing of the write is left beside it");
        assertFalse(Files.exists(root.resolve(WorkingCopy.PREVIOUS)));
    }

    @Test
    void anythingElseInTheFolderIsLeftAloneAndNotRead() throws IOException {
        // WHY: the Bind Editor keeps its own copies at the top of this folder until Storage S5.
        write(root.resolve("Custom.4.2.binds"), "<the Bind Editor's own copy/>");
        write(root.resolve("history/old.bak"), "kept");
        WorkingCopy workingCopy = new WorkingCopy(root);
        workingCopy.write(set(BOM_BINDS, "<Mappings/>", "<LVWAP/>"));
        write(root.resolve("Bindings/Custom.4.2.binds.backup"), "not a game input configuration file");
        write(root.resolve("ControlSchemes/DeviceButtonMaps/notes.txt"), "nor is this");

        workingCopy.write(set(bytes("<new/>"), "<Mappings/>", "<LVWAP/>"));

        assertEquals("<the Bind Editor's own copy/>", Files.readString(root.resolve("Custom.4.2.binds")));
        assertEquals("kept", Files.readString(root.resolve("history/old.bak")));
        ConfigurationFiles read = workingCopy.read();
        assertEquals(2, read.bindings().size(), "only binds and preset files are the working copy's");
        assertEquals(1, read.buttonMaps().size());
    }

    @Test
    void beforeFirstSetupThereIsNone() throws IOException {
        WorkingCopy workingCopy = new WorkingCopy(root);

        assertFalse(workingCopy.exists());
        assertThrows(NoSuchFileException.class, workingCopy::read);
    }

    @Test
    void aWriteThatStoppedBeforeItWasCompleteLeavesTheOldSet() throws IOException {
        WorkingCopy workingCopy = new WorkingCopy(root);
        workingCopy.write(set(bytes("<old/>"), "<Mappings/>", "<LVWAP/>"));
        // A write that stopped while filling the next set, before marking it complete.
        write(root.resolve(WorkingCopy.NEXT).resolve("Bindings/Custom.4.2.binds"), "<half/>");

        assertEquals("<old/>", text(workingCopy.read().bindings().get("Custom.4.2.binds")));
        assertFalse(Files.exists(root.resolve(WorkingCopy.NEXT)), "the half-written set is thrown away");
    }

    @Test
    void aWriteThatStoppedPartWayThroughTheSwapIsFinishedByTheNextRead() throws IOException {
        WorkingCopy workingCopy = new WorkingCopy(root);
        workingCopy.write(set(bytes("<old/>"), "<OldMappings/>", "<old/>"));
        // A write that marked the next set complete, swapped Bindings in, and stopped before ControlSchemes.
        Path next = root.resolve(WorkingCopy.NEXT);
        write(next.resolve("ControlSchemes/DeviceMappings.xml"), "<NewMappings/>");
        Files.createDirectories(next.resolve("ControlSchemes/DeviceButtonMaps"));
        write(next.resolve(WorkingCopy.COMPLETE), "");
        Path previous = root.resolve(WorkingCopy.PREVIOUS);
        Files.createDirectories(previous);
        Files.move(root.resolve("Bindings"), previous.resolve("Bindings"));
        write(root.resolve("Bindings/Custom.4.2.binds"), "<new/>");

        ConfigurationFiles read = workingCopy.read();

        assertEquals("<new/>", text(read.bindings().get("Custom.4.2.binds")));
        assertEquals("<NewMappings/>", text(read.deviceMappings()));
        assertTrue(read.buttonMaps().isEmpty(), "all of the new set, none of the old");
        assertFalse(Files.exists(next));
        assertFalse(Files.exists(previous));
    }

    @Test
    void aNameThatIsAPathOrTheWrongKindIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationFiles(Map.of("..\\Custom.4.2.binds", bytes("x")), null, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationFiles(Map.of("C:Custom.4.2.binds", bytes("x")), null, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationFiles(Map.of("notes.txt", bytes("x")), null, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationFiles(Map.of(), null, Map.of("LVWAP.xml", bytes("x"))));
        assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationFiles(Map.of(), null, Map.of("../LVWAP.buttonMap", bytes("x"))));
    }

    static ConfigurationFiles set(byte[] binds, String deviceMappings, String buttonMap) {
        return new ConfigurationFiles(
                Map.of("Custom.4.2.binds", binds, "StartPreset.4.start", bytes("Custom\r\nCustom\r\nCustom\r\nCustom")),
                bytes(deviceMappings), Map.of("LVWAP.buttonMap", bytes(buttonMap)));
    }

    static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    static String text(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private static Map<String, String> texts(Map<String, byte[]> files) {
        Map<String, String> texts = new TreeMap<>();
        files.forEach((name, content) -> texts.put(name, text(content)));
        return texts;
    }

    static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
