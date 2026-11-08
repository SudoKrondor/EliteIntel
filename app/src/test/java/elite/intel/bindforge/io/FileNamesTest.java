package elite.intel.bindforge.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /** Two names that differ only in case are one file; anything more is another (Storage S13a). */
    @Test
    void namesDifferingOnlyInCaseAreOneFile() {
        assertTrue(FileNames.sameFile("Custom.4.2.binds", "custom.4.2.BINDS"));
        assertTrue(FileNames.sameFile("DeviceMappings.xml", "devicemappings.xml"));
        assertFalse(FileNames.sameFile("Custom.4.2.binds", "Cust_om.4.2.binds"));
        assertEquals(0, FileNames.ONE_FILE.compare("LVWAP.buttonMap", "lvwap.buttonMap"));
    }

    /** A live spelling pairs with the working copy's and is held under it (Alan, 2026-11-07, Storage S13a D2). */
    @Test
    void aFileInAnotherCaseIsHeldUnderThePreferredSpelling() {
        FileNames.OneSpelling<String> one = FileNames.oneSpelling(
                Map.of("custom.4.2.binds", "theirs", "Other.4.2.binds", "other"), List.of("Custom.4.2.binds"));

        assertEquals(Map.of("Custom.4.2.binds", "theirs", "Other.4.2.binds", "other"), one.files());
        assertEquals(List.of(), one.leftOut());
    }

    /**
     * Two spellings of one file: the one spelled as preferred is kept, else the first in plain name order, and the
     * others named (Alan, 2026-11-07, Storage S13a D3).
     */
    @Test
    void ofTwoSpellingsThePreferredOneIsKept() {
        Map<String, String> both = Map.of("Custom.4.2.binds", "upper", "custom.4.2.binds", "lower");

        FileNames.OneSpelling<String> preferred = FileNames.oneSpelling(both, List.of("custom.4.2.binds"));
        assertEquals(Map.of("custom.4.2.binds", "lower"), preferred.files());
        assertEquals(List.of("Custom.4.2.binds"), preferred.leftOut());

        FileNames.OneSpelling<String> none = FileNames.oneSpelling(both, List.of());
        assertEquals(Map.of("Custom.4.2.binds", "upper"), none.files(), "the first in plain name order");
        assertEquals(List.of("custom.4.2.binds"), none.leftOut());
    }

    /** One file each, keyed ignoring case; two spellings of one file refused, never one dropped (Storage S13aR, D2). */
    @Test
    void oneFileEachRefusesTwoSpellings() {
        Map<String, String> one = FileNames.oneFileEach(Map.of("LVWAP.buttonMap", "maps"));
        assertEquals("maps", one.get("lvwap.buttonMap"), "looked up ignoring case");
        assertEquals(List.of("LVWAP.buttonMap"), List.copyOf(one.keySet()), "the spelling given");

        assertThrows(IllegalArgumentException.class,
                () -> FileNames.oneFileEach(Map.of("LVWAP.buttonMap", "upper", "lvwap.buttonMap", "lower")));
    }

    /**
     * A plain binds file: {@code <name>.binds}, no period in the name (Alan, 2026-11-07, Storage S13b Q1). A copy the
     * Explorer named, a backup and a binds file are not.
     */
    @Test
    void aPlainBindsFileHasNoPeriodInItsName() {
        assertTrue(FileNames.isPlainBindsFile("KeyboardMouseOnly.binds"));
        assertTrue(FileNames.isPlainBindsFile("KeyboardMouseOnly - Copy.binds"));
        assertFalse(FileNames.isPlainBindsFile("Custom.4.2.binds"));
        assertFalse(FileNames.isPlainBindsFile("Custom.4.2 (2).binds"));
        assertFalse(FileNames.isPlainBindsFile("Custom.4.2.binds.backup"));
        assertFalse(FileNames.isPlainBindsFile(".binds"));
        assertFalse(FileNames.isPlainBindsFile("KeyboardMouseOnly.BINDS"), "the kind is matched exactly");
        assertFalse(FileNames.isBindingsFile(Path.of("KeyboardMouseOnly.binds")), "never a binds file");
        assertEquals(Optional.of("KeyboardMouseOnly - Copy"), FileNames.plainBindName("KeyboardMouseOnly - Copy.binds"));
        assertEquals(Optional.empty(), FileNames.plainBindName("Custom.4.2.binds"));
    }

    @Test
    void aPresetFilesGenerationIsItsNumber() {
        assertEquals(4, FileNames.presetGeneration("StartPreset.4.start"));
        assertEquals(-1, FileNames.presetGeneration("StartPreset.start"));
    }
}
