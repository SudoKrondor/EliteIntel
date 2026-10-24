package elite.intel.bindforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Anything wrong with the chosen backup folder falls back to Elite-Intel's default rather than failing the backup
 * (file-manager.md, <em>Settings</em>).
 */
class BackupDestinationTest {

    @TempDir
    Path temp;

    @Test
    void noChoiceIsTheDefault() throws IOException {
        assertEquals(defaultFolder(), destination(() -> null, List.of()).resolve());
        assertEquals(defaultFolder(), destination(() -> "  ", List.of()).resolve());
    }

    @Test
    void theChosenFolderIsUsedAndCreated() throws IOException {
        Path chosen = temp.resolve("my backups");

        assertEquals(chosen, destination(chosen::toString, List.of()).resolve());
        assertTrue(Files.isDirectory(chosen));
    }

    @Test
    void aFolderInsideAGameFolderIsNeverUsed() throws IOException {
        Path game = temp.resolve("Elite Dangerous");
        Path chosen = game.resolve("Backups");

        assertEquals(defaultFolder(), destination(chosen::toString, List.of(game)).resolve(),
                "a game update would delete it");
    }

    @Test
    void aSettingThatCannotBeReadIsTheDefault() throws IOException {
        BackupDestination destination = destination(() -> {
            throw new IllegalStateException("database is gone");
        }, List.of());

        assertEquals(defaultFolder(), destination.resolve());
    }

    @Test
    void aFolderThatCannotBeMadeIsTheDefault() throws IOException {
        Path aFile = temp.resolve("a file");
        Files.writeString(aFile, "not a folder");

        assertEquals(defaultFolder(), destination(() -> aFile.resolve("Backups").toString(), List.of()).resolve());
    }

    @Test
    void gameFoldersThatCannotBeReadDoNotStopTheBackup() throws IOException {
        Path chosen = temp.resolve("my backups");
        BackupDestination destination = new BackupDestination(chosen::toString, this::defaultFolder, () -> {
            throw new IllegalStateException("database is gone");
        });

        assertEquals(chosen, destination.resolve());
    }

    private BackupDestination destination(BackupDestination.Chosen chosen, List<Path> gameFolders) {
        return new BackupDestination(chosen, this::defaultFolder, () -> gameFolders);
    }

    private Path defaultFolder() throws IOException {
        return Files.createDirectories(temp.resolve("playerbackups"));
    }
}
