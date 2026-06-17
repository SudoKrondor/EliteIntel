package elite.intel.ai.hands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingsWorkingCopyRepositoryTest {

    private static final String PRESET = "Custom.3.0.binds";

    @TempDir
    Path tempDir;

    @Test
    void gameOnlyChangeRefreshesCleanWorkingCopyWithoutCreatingDraft() throws Exception {
        Path gameFile = gameFile();
        String original = binds("Key_A");
        String changedByGame = binds("Key_B");
        write(gameFile, original);

        BindingsWorkingCopyRepository repo = repo();
        Path workingCopy = repo.loadOrImportFromGame(PRESET, gameFile);
        write(gameFile, changedByGame);

        assertFalse(repo.hasUnappliedDraft(PRESET, gameFile));
        assertEquals(changedByGame, Files.readString(workingCopy, StandardCharsets.UTF_8));
    }

    @Test
    void dirtyWorkingCopyIsPreservedWhenGameChangesInParallel() throws Exception {
        Path gameFile = gameFile();
        write(gameFile, binds("Key_A"));

        BindingsWorkingCopyRepository repo = repo();
        Path workingCopy = repo.loadOrImportFromGame(PRESET, gameFile);
        String eiDraft = binds("Key_E");
        String changedByGame = binds("Key_G");
        write(workingCopy, eiDraft);
        write(gameFile, changedByGame);

        assertTrue(repo.hasUnappliedDraft(PRESET, gameFile));
        assertFalse(repo.gameFileMatchesBaseline(PRESET, gameFile));
        assertEquals(eiDraft, Files.readString(workingCopy, StandardCharsets.UTF_8));
    }

    private BindingsWorkingCopyRepository repo() {
        return new BindingsWorkingCopyRepository(tempDir.resolve("working"));
    }

    private Path gameFile() throws Exception {
        Path gameFile = tempDir.resolve("game").resolve(PRESET);
        Files.createDirectories(gameFile.getParent());
        return gameFile;
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
