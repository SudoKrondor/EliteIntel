package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static elite.intel.bindforge.storage.WorkingCopyTest.bytes;
import static elite.intel.bindforge.storage.WorkingCopyTest.text;
import static elite.intel.bindforge.storage.WorkingCopyTest.write;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The one write: back up, save the working copy, write the live copies - in that order, and nothing after a step that
 * failed (principle 3; storage.md rules 7-8).
 */
class WritePathTest {

    @TempDir
    Path temp;

    private Path bindings;
    private Path steam;
    private WorkingCopy workingCopy;
    private final List<String> order = new ArrayList<>();

    @BeforeEach
    void aBindingsFolderAnInstallationAndAnEmptyWorkingCopy() throws IOException {
        bindings = temp.resolve("Options/Bindings");
        write(bindings.resolve("Custom.4.2.binds"), "<live/>");
        steam = temp.resolve("Steam/EliteDangerous");
        Files.createDirectories(GameInstallation.controlSchemesUnder(steam));
        workingCopy = new WorkingCopy(temp.resolve("working"));
    }

    @Test
    void theBackupIsTakenFirstThenTheWorkingCopyAndTheLiveCopiesHoldTheSet() throws IOException {
        WriteReport report = writePath().write(set(), () -> {
            assertFalse(workingCopy.exists(), "the backup comes before anything is written");
            assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
            order.add("backup");
            return backup();
        });

        assertEquals(List.of("backup"), order);
        assertTrue(report.complete());
        assertEquals("<new/>", text(workingCopy.read().bindings().get("Custom.4.2.binds")));
        assertEquals("<new/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertEquals("<Mappings/>",
                Files.readString(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(steam))));
    }

    @Test
    void aBackupThatFailsStopsTheWriteWithNothingWritten() throws IOException {
        assertThrows(IOException.class, () -> writePath().write(set(), () -> {
            throw new IOException("the backup folder cannot be written");
        }));

        assertFalse(workingCopy.exists());
        assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
    }

    @Test
    void aWorkingCopyThatCannotBeSavedStopsTheWriteBeforeAnyLiveCopy() throws IOException {
        // WHY: a file where the working copy's folder goes, so it cannot be saved.
        Path blocked = temp.resolve("blocked");
        Files.writeString(blocked, "in the way");
        WritePath writePath = new WritePath(new WorkingCopy(blocked), this::sources);

        assertThrows(IOException.class, () -> writePath.write(set(), this::backup));

        assertEquals("<live/>", Files.readString(bindings.resolve("Custom.4.2.binds")));
        assertFalse(Files.exists(GameInstallation.deviceMappingsIn(GameInstallation.controlSchemesUnder(steam))));
    }

    private WritePath writePath() {
        return new WritePath(workingCopy, this::sources);
    }

    private BackupSources sources() {
        return new BackupSources(bindings, List.of(new BackupSources.Installation("STEAM", steam, false, null)));
    }

    private Backup backup() {
        return new Backup(temp.resolve("backups/BeforeChanges/2026-10-25_10-00-00.zip"), LocalDateTime.now(),
                Backups.Kind.BEFORE_CHANGES, Backups.Before.APPLY, true, List.of());
    }

    private static ConfigurationFiles set() {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", bytes("<new/>")), bytes("<Mappings/>"), Map.of());
    }
}
