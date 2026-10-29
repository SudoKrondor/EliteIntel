package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The name rules of the game input configuration files, in their one home (review, 2026-10-29). */
class FileNamesTest {

    @Test
    void aBindsFileCarriesItsVersion() {
        assertTrue(FileNames.isBindsFile("Custom.4.2.binds"));
        assertFalse(FileNames.isBindsFile("KeyboardMouseOnly.binds"), "a factory binds file carries no version");
        assertFalse(FileNames.isBindsFile("Custom.4.2.binds.backup"));
        assertFalse(FileNames.isBindsFile("StartPreset.4.start"));
    }

    @Test
    void aPresetFileIsTheNumberedOne() {
        assertTrue(FileNames.isPresetFile("StartPreset.4.start"));
        assertFalse(FileNames.isPresetFile("StartPreset.start"), "the pre-Odyssey name");
        assertTrue(FileNames.isBindingsFile(Path.of("StartPreset.4.start")));
        assertTrue(FileNames.isBindingsFile(Path.of("Custom.4.2.binds")));
        assertFalse(FileNames.isBindingsFile(Path.of("DeviceMappings.xml")));
    }

    @Test
    void aBindNameIsTheFileNameBeforeItsVersion() {
        assertEquals(Optional.of("Dual.Virpil"), FileNames.bindName("Dual.Virpil.4.2.binds"));
        assertEquals(Optional.empty(), FileNames.bindName("StartPreset.4.start"));
    }

    @Test
    void versionsCompareMajorBeforeMinor() {
        assertTrue(FileNames.version("Custom.4.10.binds") > FileNames.version("Custom.4.2.binds"));
        assertTrue(FileNames.version("Custom.5.0.binds") > FileNames.version("Custom.4.999.binds"));
        assertEquals(-1, FileNames.version("KeyboardMouseOnly.binds"));
    }

    @Test
    void aVersionTooLongToReadCountsAsTheLargest() {
        assertEquals(FileNames.version("Custom.4.999999.binds"), FileNames.version("Custom.4.12345678.binds"));
        assertEquals(999_999, FileNames.presetGeneration("StartPreset.12345678.start"));
    }

    @Test
    void aPresetFilesGenerationIsItsNumber() {
        assertEquals(4, FileNames.presetGeneration("StartPreset.4.start"));
        assertEquals(-1, FileNames.presetGeneration("StartPreset.start"));
    }
}
