package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingsApplyServiceTest {

    private static final String PRESET = "Custom.3.0.binds";

    @TempDir
    Path tempDir;

    @Test
    void applyDirtyDraftWritesGameFileAndUpdatesBaseline() throws Exception {
        Path gameFile = gameFile();
        String original = binds("Key_A");
        String eiDraft = binds("Key_E");
        write(gameFile, original);

        BindingsWorkingCopyRepository repo = repo();
        Path workingCopy = repo.loadOrImportFromGame(PRESET, gameFile);
        write(workingCopy, eiDraft);

        Path backup = service(repo).apply(PRESET, gameFile);

        assertNotNull(backup);
        assertEquals(original, Files.readString(backup, StandardCharsets.UTF_8));
        assertEquals(eiDraft, Files.readString(gameFile, StandardCharsets.UTF_8));
        assertFalse(repo.hasUnappliedDraft(PRESET, gameFile));
    }

    @Test
    void applyWithoutEiDraftAfterGameChangeDoesNotRestoreOldWorkingCopy() throws Exception {
        Path gameFile = gameFile();
        String original = binds("Key_A");
        String changedByGame = binds("Key_G");
        write(gameFile, original);

        BindingsWorkingCopyRepository repo = repo();
        Path workingCopy = repo.loadOrImportFromGame(PRESET, gameFile);
        write(gameFile, changedByGame);

        Path backup = service(repo).apply(PRESET, gameFile);

        assertNull(backup);
        assertEquals(changedByGame, Files.readString(gameFile, StandardCharsets.UTF_8));
        assertEquals(changedByGame, Files.readString(workingCopy, StandardCharsets.UTF_8));
        assertTrue(backups().isEmpty());
        assertFalse(repo.hasUnappliedDraft(PRESET, gameFile));
    }

    @Test
    void applyDirtyDraftFailsWhenGameFileChangedSinceBaseline() throws Exception {
        Path gameFile = gameFile();
        String original = binds("Key_A");
        String eiDraft = binds("Key_E");
        String changedByGame = binds("Key_G");
        write(gameFile, original);

        BindingsWorkingCopyRepository repo = repo();
        Path workingCopy = repo.loadOrImportFromGame(PRESET, gameFile);
        write(workingCopy, eiDraft);
        write(gameFile, changedByGame);

        BindingsApplyException thrown = assertThrows(
                BindingsApplyException.class,
                () -> service(repo).apply(PRESET, gameFile));

        assertEquals("bindings.apply.conflict", thrown.localizationKey());
        assertEquals(changedByGame, Files.readString(gameFile, StandardCharsets.UTF_8));
        assertTrue(backups().isEmpty());
    }

    private BindingsWorkingCopyRepository repo() {
        return new BindingsWorkingCopyRepository(tempDir.resolve("working"));
    }

    private BindingsApplyService service(BindingsWorkingCopyRepository repo) {
        return new BindingsApplyService(repo, new BindingsBackupService(), tempDir.resolve("backups"));
    }

    private Path gameFile() throws Exception {
        Path gameFile = tempDir.resolve("game").resolve(PRESET);
        Files.createDirectories(gameFile.getParent());
        return gameFile;
    }

    private List<Path> backups() throws Exception {
        Path backupDir = tempDir.resolve("backups");
        if (!Files.exists(backupDir)) {
            return List.of();
        }
        try (var stream = Files.list(backupDir)) {
            return stream.toList();
        }
    }

    private void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String binds(String key) {
        return """
                <Root>
                    <FocusLeftPanel>
                        <Primary Device="Keyboard" Key="%s" />
                        <Secondary Device="{NoDevice}" Key="" />
                    </FocusLeftPanel>
                </Root>
                """.formatted(key);
    }
}
