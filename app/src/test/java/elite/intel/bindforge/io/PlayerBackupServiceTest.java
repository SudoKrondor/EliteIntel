package elite.intel.bindforge.io;

import elite.intel.io.TimestampedBackups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Restore to Editing Slot and Restore to Live, until RESTORE replaces them (Storage S3). They read a backup taken
 * since Storage S1, which keeps its {@code .binds} under {@code Bindings\}, and an old folder backup, which keeps them
 * at its top. Taking backups is {@code BackupsTest}'s.
 */
class PlayerBackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void restoreToWorkingCopyReadsANewBackupsBindingsFolder() throws Exception {
        Path backupFolder = newBackup("Custom.3.0.binds", binds("A"));
        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();

        service(workingCopyRepo).restoreToWorkingCopy(backupFolder, "Custom.3.0.binds");

        assertEquals(binds("A"), Files.readString(workingCopyRepo.getWorkingCopyPath("Custom.3.0.binds"),
                StandardCharsets.UTF_8));
    }

    @Test
    void restoreToWorkingCopyReadsAnOldFolderBackup() throws Exception {
        Path backupFolder = oldBackup("Custom.3.0.binds", binds("V11"));
        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();

        service(workingCopyRepo).restoreToWorkingCopy(backupFolder, "Custom.3.0.binds");

        assertEquals(binds("V11"), Files.readString(workingCopyRepo.getWorkingCopyPath("Custom.3.0.binds"),
                StandardCharsets.UTF_8));
    }

    @Test
    void restoreToWorkingCopyThrowsWhenBackupHasNoFileForThatPreset() throws Exception {
        Path backupFolder = newBackup("Custom.3.0.binds", binds("A"));

        assertThrows(IOException.class,
                () -> service(workingCopyRepo()).restoreToWorkingCopy(backupFolder, "DawnTreader.4.0.binds"));
    }

    @Test
    void restoreToLiveResultsInSyncedStateNotDraft() throws Exception {
        Path backupFolder = newBackup("Custom.3.0.binds", binds("A"));
        Path gameFile = liveFile("Custom.3.0.binds", binds("Z"));
        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();

        service(workingCopyRepo).restoreToLive(backupFolder, "Custom.3.0.binds", gameFile);

        assertFalse(workingCopyRepo.hasUnappliedDraft("Custom.3.0.binds", gameFile),
                "Working copy should be in synced state after restore-to-live, not still showing as a draft");
    }

    @Test
    void restoreToLiveReturnsNullWithoutThrowingWhenGameFileDoesNotExist() throws Exception {
        Path backupFolder = newBackup("Custom.3.0.binds", binds("A"));
        Path nonExistentGameFile = tempDir.resolve("nonexistent").resolve("Custom.3.0.binds");

        Path result = service(workingCopyRepo()).restoreToLive(backupFolder, "Custom.3.0.binds", nonExistentGameFile);

        assertNull(result, "restoreToLive should return null (not throw) when the game file does not exist");
    }

    @Test
    void restoreToLiveAppliesTheRestoredDraftThroughTheSafeApplyPipeline() throws Exception {
        Path backupFolder = newBackup("Custom.3.0.binds", binds("A"));
        Path gameFile = liveFile("Custom.3.0.binds", binds("Z")); // live file has since diverged from the backup

        Path applyBackup = service(workingCopyRepo()).restoreToLive(backupFolder, "Custom.3.0.binds", gameFile);

        assertNotNull(applyBackup);
        assertEquals(binds("A"), Files.readString(gameFile, StandardCharsets.UTF_8));
        assertEquals(binds("Z"), Files.readString(applyBackup, StandardCharsets.UTF_8));
    }

    private Path newBackup(String name, String content) throws IOException {
        Path folder = tempDir.resolve("backups").resolve("2026-10-24_18-30-00");
        write(folder.resolve("Bindings").resolve(name), content);
        write(folder.resolve("backup-info.txt"), "Status: Full");
        return folder;
    }

    private Path oldBackup(String name, String content) throws IOException {
        Path folder = tempDir.resolve("backups").resolve("2026-06-24_18-30-00");
        write(folder.resolve(name), content);
        return folder;
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
