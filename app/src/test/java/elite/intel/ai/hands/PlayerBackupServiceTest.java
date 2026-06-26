package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBackupServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createBackupSweepsBindsFilesAndStartPresetIntoTimestampedFolder() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        write(bindingsDir.resolve("Custom.3.0.binds"), "<Root/>");
        write(bindingsDir.resolve("DawnTreader.4.0.binds"), "<Root/>");
        write(bindingsDir.resolve("StartPreset.4.start"), "DawnTreader\nDawnTreader\n");
        write(bindingsDir.resolve("DeviceMappings.xml"), "<ignored/>"); // out of scope - must not be swept

        Path playerBackupsDir = tempDir.resolve("playerbackups");
        PlayerBackupService service = service(playerBackupsDir, fixedClock("2026-06-24T18:30:00Z"));

        Path backupFolder = service.createBackup(bindingsDir);

        assertEquals(playerBackupsDir.resolve("2026-06-24_18-30-00"), backupFolder);
        assertTrue(Files.exists(backupFolder.resolve("Custom.3.0.binds")));
        assertTrue(Files.exists(backupFolder.resolve("DawnTreader.4.0.binds")));
        assertTrue(Files.exists(backupFolder.resolve("StartPreset.4.start")));
        assertEquals(3, countEntries(backupFolder));
    }

    @Test
    void secondBackupInTheSameSecondGetsACollisionSafeFolderName() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        write(bindingsDir.resolve("Custom.3.0.binds"), "<Root/>");

        Path playerBackupsDir = tempDir.resolve("playerbackups");
        PlayerBackupService service = service(playerBackupsDir, fixedClock("2026-06-24T18:30:00Z"));

        Path first = service.createBackup(bindingsDir);
        Path second = service.createBackup(bindingsDir);

        assertEquals(playerBackupsDir.resolve("2026-06-24_18-30-00"), first);
        assertEquals(playerBackupsDir.resolve("2026-06-24_18-30-00-1"), second);
    }

    @Test
    void listBackupsReturnsNewestFirstWithFileNames() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        write(bindingsDir.resolve("Custom.3.0.binds"), "<Root/>");

        Path playerBackupsDir = tempDir.resolve("playerbackups");
        service(playerBackupsDir, fixedClock("2026-06-24T18:00:00Z")).createBackup(bindingsDir);
        PlayerBackupService later = service(playerBackupsDir, fixedClock("2026-06-24T19:00:00Z"));
        later.createBackup(bindingsDir);

        List<PlayerBackupService.PlayerBackup> backups = later.listBackups();

        assertEquals(2, backups.size());
        assertEquals("2026-06-24_19-00-00", backups.get(0).timestamp());
        assertEquals("2026-06-24_18-00-00", backups.get(1).timestamp());
        assertEquals(List.of("Custom.3.0.binds"), backups.get(0).fileNames());
    }

    @Test
    void createBackupThrowsWhenBindingsDirHasNothingToBackUp() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        write(bindingsDir.resolve("DeviceMappings.xml"), "<ignored/>"); // out of scope, doesn't count

        PlayerBackupService service = service(tempDir.resolve("playerbackups"), fixedClock("2026-06-24T18:30:00Z"));

        assertThrows(IOException.class, () -> service.createBackup(bindingsDir));
    }

    @Test
    void listBackupsReturnsEmptyListWhenNoBackupsExistYet() throws Exception {
        PlayerBackupService service = service(tempDir.resolve("playerbackups"), fixedClock("2026-06-24T18:30:00Z"));

        assertTrue(service.listBackups().isEmpty());
    }

    @Test
    void restoreToWorkingCopyLoadsTheBackupFileAsTheNewDraft() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        String originalContent = binds("A");
        write(bindingsDir.resolve("Custom.3.0.binds"), originalContent);

        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();
        PlayerBackupService service = service(tempDir.resolve("playerbackups"), fixedClock("2026-06-24T18:30:00Z"), workingCopyRepo);
        Path backupFolder = service.createBackup(bindingsDir);

        service.restoreToWorkingCopy(backupFolder, "Custom.3.0.binds");

        Path workingCopy = workingCopyRepo.getWorkingCopyPath("Custom.3.0.binds");
        assertEquals(originalContent, Files.readString(workingCopy, StandardCharsets.UTF_8));
    }

    @Test
    void restoreToWorkingCopyThrowsWhenBackupHasNoFileForThatPreset() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        write(bindingsDir.resolve("Custom.3.0.binds"), binds("A"));

        PlayerBackupService service = service(tempDir.resolve("playerbackups"), fixedClock("2026-06-24T18:30:00Z"));
        Path backupFolder = service.createBackup(bindingsDir);

        assertThrows(IOException.class, () -> service.restoreToWorkingCopy(backupFolder, "DawnTreader.4.0.binds"));
    }

    @Test
    void restoreToLiveAppliesTheRestoredDraftThroughTheSafeApplyPipeline() throws Exception {
        Path bindingsDir = tempDir.resolve("bindings");
        Files.createDirectories(bindingsDir);
        String originalContent = binds("A");
        write(bindingsDir.resolve("Custom.3.0.binds"), originalContent);

        BindingsWorkingCopyRepository workingCopyRepo = workingCopyRepo();
        PlayerBackupService service = service(tempDir.resolve("playerbackups"), fixedClock("2026-06-24T18:30:00Z"), workingCopyRepo);
        Path backupFolder = service.createBackup(bindingsDir);

        Path gameFile = bindingsDir.resolve("Custom.3.0.binds");
        write(gameFile, binds("Z")); // live file has since diverged from the backup

        Path applyBackup = service.restoreToLive(backupFolder, "Custom.3.0.binds", gameFile);

        assertNotNull(applyBackup);
        assertEquals(originalContent, Files.readString(gameFile, StandardCharsets.UTF_8));
        assertEquals(binds("Z"), Files.readString(applyBackup, StandardCharsets.UTF_8));
    }

    private PlayerBackupService service(Path playerBackupsDir, Clock clock) {
        return service(playerBackupsDir, clock, workingCopyRepo());
    }

    private PlayerBackupService service(Path playerBackupsDir, Clock clock, BindingsWorkingCopyRepository workingCopyRepo) {
        BindingsApplyService applyService =
                new BindingsApplyService(workingCopyRepo, new BindingsBackupService(), tempDir.resolve("applybackups"));
        return new PlayerBackupService(new BindingsLoader(), workingCopyRepo, applyService, clock, playerBackupsDir);
    }

    private BindingsWorkingCopyRepository workingCopyRepo() {
        return new BindingsWorkingCopyRepository(tempDir.resolve("working"));
    }

    private String binds(String key) {
        return "<Root Key=\"" + key + "\"/>";
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private long countEntries(Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }

    private void write(Path file, String content) throws Exception {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
