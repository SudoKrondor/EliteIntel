package elite.intel.bindforge.storage;

import elite.intel.bindforge.install.GameInstallation;
import elite.intel.db.dao.BindForgeInstallationsDao.InstallationRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * First-time startup's flow (storage.md rules 1-4; Storage S2b, Alan, 2026-10-25): the flag, the reading, the write
 * through the write path with the first setup backup, and the new player who has no bindings folder yet.
 */
class FirstTimeStartupTest {

    @TempDir
    Path temp;

    private final FakeFlag flag = new FakeFlag();
    private final List<String> steps = new ArrayList<>();
    private final List<Backup> announced = new ArrayList<>();
    private final List<InstallationRow> rows = new ArrayList<>();

    @Test
    void itIsNeededUntilTheFlagIsSet() {
        FirstTimeStartup startup = startup(temp.resolve("Bindings"), writerThatWrites());
        assertTrue(startup.needed());

        flag.complete = true;

        assertFalse(startup.needed());
    }

    /** A new player before the training: nothing of theirs exists yet, so it does not run (Alan, 2026-10-25). */
    @Test
    void aMissingBindingsFolderIsANewPlayerAndNothingIsRead() {
        FirstTimeStartup startup = startup(temp.resolve("no such folder"), writerThatWrites());

        assertThrows(FirstTimeStartup.NoBindingsYetException.class, startup::read);
        assertFalse(flag.complete);
    }

    @Test
    void readingTakesEveryBindsAndPresetFileAndEveryReachableInstallation() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Files.writeString(bindings.resolve("Custom.4.2.binds"), "<Root />");
        Files.writeString(bindings.resolve("StartPreset.4.start"), "Custom");
        Files.writeString(bindings.resolve("Custom.4.2.binds.backup"), "not one of the files");
        Path steam = temp.resolve("steam");
        Path controlSchemes = Files.createDirectories(GameInstallation.controlSchemesUnder(steam));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), "<Root />");
        rows.add(new InstallationRow(1, "STEAM", steam.toString(), false, false));
        rows.add(new InstallationRow(2, "EPIC", temp.resolve("unplugged").toString(), false, true));

        FirstSetupReading reading = startup(bindings, writerThatWrites()).read();

        assertEquals(List.of("Custom.4.2.binds", "StartPreset.4.start"), List.copyOf(reading.bindings().keySet()));
        assertEquals(List.of(1L), reading.installations().stream().map(InstallationFiles::installId).toList());
        assertEquals(List.of(2L), reading.unreachable().stream().map(InstallationRow::id).toList());
    }

    /**
     * Review, 2026-10-25 (Alan): installations known but none reachable - a drive unplugged - stop first-time startup,
     * naming them. A working copy with no device files would otherwise go over theirs when they come back.
     */
    @Test
    void installationsKnownButNoneReachableStopTheReadingAndAreNamed() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        rows.add(new InstallationRow(1, "STEAM", temp.resolve("unplugged").toString(), false, true));

        FirstTimeStartup.NoInstallationReachableException thrown = assertThrows(
                FirstTimeStartup.NoInstallationReachableException.class,
                () -> startup(bindings, writerThatWrites()).read());

        assertEquals(List.of(1L), thrown.unreachable().stream().map(InstallationRow::id).toList());
        assertFalse(flag.complete);
    }

    @Test
    void withNoInstallationKnownTheReadingGoesOn() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));

        FirstSetupReading reading = startup(bindings, writerThatWrites()).read();

        assertTrue(reading.installations().isEmpty());
    }

    /** An unread file could hold the one entry the working copy must not lose, so nothing goes on without it. */
    @Test
    void anUnreadableDeviceFileStopsTheReadingAndNamesTheInstallation() throws IOException {
        Path bindings = Files.createDirectories(temp.resolve("Bindings"));
        Path steam = temp.resolve("steam");
        Path controlSchemes = Files.createDirectories(GameInstallation.controlSchemesUnder(steam));
        Files.writeString(controlSchemes.resolve("DeviceMappings.xml"), "<Root><LVWAP>");
        rows.add(new InstallationRow(1, "STEAM", steam.toString(), false, false));

        IOException thrown = assertThrows(IOException.class, () -> startup(bindings, writerThatWrites()).read());

        assertTrue(thrown.getMessage().contains("STEAM") && thrown.getMessage().contains("DeviceMappings.xml"),
                thrown.getMessage());
    }

    /** Back up first, then the write - and only then the flag and the one spoken line (alias-designer.md). */
    @Test
    void aWriteBacksUpFirstThenSetsTheFlagAndSaysSoOnce() throws IOException {
        FirstTimeStartup startup = startup(temp.resolve("Bindings"), writerThatWrites());

        FirstTimeStartup.Completed completed = startup.write(files());
        Written written = completed.written();

        assertEquals(List.of("backup", "write"), steps);
        assertTrue(flag.complete);
        assertFalse(completed.setUpAskedAgain());
        assertEquals(List.of(written.backup()), announced);
        assertEquals(Backups.Kind.FIRST_SETUP, written.backup().kind());
    }

    @Test
    void aWriteThatFailsLeavesTheFlagUnsetAndSaysNothing() {
        FirstTimeStartup startup = startup(temp.resolve("Bindings"), (files, backUp) -> {
            throw new IOException("the working copy could not be saved");
        });

        assertThrows(IOException.class, () -> startup.write(files()));
        assertFalse(flag.complete);
        assertTrue(announced.isEmpty());
    }

    /**
     * The flag failing after the files are written is reported with the write, not thrown - thrown, the user was told
     * nothing was written (review, 2026-10-27). The backup was taken, so it is still announced.
     */
    @Test
    void aFlagThatCannotBeSetIsReportedWithTheWriteNotThrown() throws IOException {
        flag.failToMark = true;
        FirstTimeStartup startup = startup(temp.resolve("Bindings"), writerThatWrites());

        FirstTimeStartup.Completed completed = startup.write(files());

        assertEquals(List.of("backup", "write"), steps);
        assertTrue(completed.setUpAskedAgain());
        assertFalse(flag.complete);
        assertEquals(List.of(completed.written().backup()), announced);
    }

    /** Run first-time startup again clears the flag and nothing else (file-manager.md, Game Install Locations). */
    @Test
    void runningItAgainClearsTheFlag() {
        flag.complete = true;
        FirstTimeStartup startup = startup(temp.resolve("Bindings"), writerThatWrites());

        startup.runAgain();

        assertTrue(startup.needed());
        assertTrue(steps.isEmpty(), "nothing is written");
    }

    // ------------------------------------------------------------------ fixtures

    private FirstTimeStartup startup(Path bindingsFolder, FirstTimeStartup.Writer writer) {
        return new FirstTimeStartup(flag, () -> bindingsFolder, () -> List.copyOf(rows), writer,
                sources -> {
                    steps.add("backup");
                    return new Backup(temp.resolve("FirstSetup").resolve("2026-10-25_10-00-00.zip"),
                            LocalDateTime.of(2026, 10, 25, 10, 0), Backups.Kind.FIRST_SETUP, null, true, List.of());
                },
                announced::add);
    }

    /** The write path's order: the backup it is handed, then the write. */
    private FirstTimeStartup.Writer writerThatWrites() {
        return (files, backUp) -> {
            backUp.take(new BackupSources(temp.resolve("Bindings"), List.of()));
            steps.add("write");
            WriteReport.Place bindings = new WriteReport.Place(WriteReport.Kind.BINDINGS_FOLDER, null,
                    temp.resolve("Bindings"), List.of(), List.of(), List.of(), List.of());
            return new WriteReport(bindings, List.of());
        };
    }

    private static ConfigurationFiles files() {
        return new ConfigurationFiles(Map.of("Custom.4.2.binds", "<Root />".getBytes(StandardCharsets.UTF_8)), null,
                Map.of());
    }

    private static final class FakeFlag implements FirstTimeStartup.Flag {
        boolean complete;
        boolean failToMark;

        @Override
        public boolean complete() {
            return complete;
        }

        @Override
        public void markComplete() {
            if (failToMark) throw new IllegalStateException("the database is locked");
            complete = true;
        }

        @Override
        public void clear() {
            complete = false;
        }
    }
}
