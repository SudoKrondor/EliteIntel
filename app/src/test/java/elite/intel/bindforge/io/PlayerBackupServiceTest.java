package elite.intel.bindforge.io;

import elite.intel.bindforge.storage.Backup;
import elite.intel.bindforge.storage.Backups;
import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Restore to Editing Slot and Restore to Live, until RESTORE replaces them (Storage S3). They read the {@code .binds}
 * under a backup zip's {@code Bindings/} (Storage S1b). Taking backups is {@code BackupsTest}'s.
 */
class PlayerBackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void restoreToWorkingCopyReadsTheBackupZipsBindingsFolder() throws Exception {
        Backup backup = newBackup("Custom.3.0.binds", binds("A"));
        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();

        service(workingCopyRepo).restoreToWorkingCopy(backup, "Custom.3.0.binds");

        assertEquals(binds("A"), Files.readString(workingCopyRepo.getWorkingCopyPath("Custom.3.0.binds"),
                StandardCharsets.UTF_8));
    }

    @Test
    void restoreToWorkingCopyThrowsWhenBackupHasNoFileForThatPreset() throws Exception {
        Backup backup = newBackup("Custom.3.0.binds", binds("A"));

        assertThrows(IOException.class,
                () -> service(workingCopyRepo()).restoreToWorkingCopy(backup, "DawnTreader.4.0.binds"));
    }

    @Test
    void restoreToLiveResultsInSyncedStateNotDraft() throws Exception {
        Backup backup = newBackup("Custom.3.0.binds", binds("A"));
        Path gameFile = liveFile("Custom.3.0.binds", binds("Z"));
        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();

        service(workingCopyRepo).restoreToLive(backup, "Custom.3.0.binds", gameFile);

        assertFalse(workingCopyRepo.hasUnappliedDraft("Custom.3.0.binds", gameFile),
                "Working copy should be in synced state after restore-to-live, not still showing as a draft");
    }

    @Test
    void restoreToLiveReturnsNullWithoutThrowingWhenGameFileDoesNotExist() throws Exception {
        Backup backup = newBackup("Custom.3.0.binds", binds("A"));
        Path nonExistentGameFile = tempDir.resolve("nonexistent").resolve("Custom.3.0.binds");

        Path result = service(workingCopyRepo()).restoreToLive(backup, "Custom.3.0.binds", nonExistentGameFile);

        assertNull(result, "restoreToLive should return null (not throw) when the game file does not exist");
    }

    @Test
    void restoreToLiveAppliesTheRestoredDraftThroughTheSafeApplyPipeline() throws Exception {
        Backup backup = newBackup("Custom.3.0.binds", binds("A"));
        Path gameFile = liveFile("Custom.3.0.binds", binds("Z")); // live file has since diverged from the backup

        Path applyBackup = service(workingCopyRepo()).restoreToLive(backup, "Custom.3.0.binds", gameFile);

        assertNotNull(applyBackup);
        assertEquals(binds("A"), Files.readString(gameFile, StandardCharsets.UTF_8));
        assertEquals(binds("Z"), Files.readString(applyBackup, StandardCharsets.UTF_8));
    }

    private Backup newBackup(String name, String content) throws IOException {
        Path zip = tempDir.resolve("backups").resolve("User").resolve("2026-10-24_18-30-00.zip");
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip), StandardCharsets.UTF_8)) {
            out.putNextEntry(new ZipEntry("Bindings/" + name));
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry(Backups.INFO_FILE));
            out.write("Status: Full".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return new Backup(zip, LocalDateTime.parse("2026-10-24T18:30:00"), Backups.Kind.USER, null, true, List.of());
    }

    private Path liveFile(String name, String content) throws IOException {
        Path file = tempDir.resolve("bindings").resolve(name);
        write(file, content);
        return file;
    }

    private PlayerBackupService service(BindingsWorkingCopyRepository workingCopyRepo) {
        BindingsApplyService applyService =
                new BindingsApplyService(workingCopyRepo, new TimestampedBackups(), tempDir.resolve("applybackups"));
        return new PlayerBackupService(workingCopyRepo, applyService);
    }

    private BindingsWorkingCopyRepository workingCopyRepo() {
        return new BindingsWorkingCopyRepository(tempDir.resolve("working"));
    }

    private static String binds(String key) {
        return "<Root Key=\"" + key + "\"/>";
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
