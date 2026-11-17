package elite.intel.junit.gameapi;

import elite.intel.gameapi.DataDirectoryValidator;
import elite.intel.gameapi.DataDirectoryValidator.DirectoryKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the directory-validation contract: {@code validateAndWarn} returns {@code true} only when the
 * directory exists and holds at least one file with the kind's suffix. The failure paths also publish
 * a spoken warning, but that is a side effect on an unsubscribed bus here; the boolean is the testable
 * contract.
 */
class DataDirectoryValidatorTest {

    @Test
    void journalDirWithLogFileIsValid(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Journal.2026-06-23T120000.01.log"));

        assertTrue(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.JOURNAL));
    }

    /** A fresh install on a stock preset: the game's folder, with its preset file and no .binds at all. */
    @Test
    void bindingsDirWithAStartPresetIsValid(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("StartPreset.4.start"), "KeyboardMouseOnly\n");

        assertTrue(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.BINDINGS));
    }

    @Test
    void bindingsDirWithOnlyBindsFilesIsNotTheGamesFolder(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Custom.4.0.binds"));

        assertFalse(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.BINDINGS));
    }

    @Test
    void suffixMatchIsCaseInsensitive(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Journal.2026-06-23T120000.01.LOG"));

        assertTrue(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.JOURNAL));
    }

    @Test
    void emptyDirIsInvalid(@TempDir Path dir) {
        assertFalse(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.JOURNAL));
    }

    @Test
    void dirWithOnlyWrongSuffixIsInvalid(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Status.json"));

        assertFalse(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.JOURNAL));
    }

    @Test
    void wrongKindRejectsOtherKindsFiles(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("Custom.4.0.binds"));

        // A bindings file does not satisfy a journal directory check.
        assertFalse(DataDirectoryValidator.validateAndWarn(dir, DirectoryKind.JOURNAL));
    }

    @Test
    void missingDirIsInvalid(@TempDir Path dir) {
        Path missing = dir.resolve("does-not-exist");

        assertFalse(DataDirectoryValidator.validateAndWarn(missing, DirectoryKind.JOURNAL));
    }

    @Test
    void nullDirIsInvalid() {
        assertFalse(DataDirectoryValidator.validateAndWarn(null, DirectoryKind.BINDINGS));
    }
}
