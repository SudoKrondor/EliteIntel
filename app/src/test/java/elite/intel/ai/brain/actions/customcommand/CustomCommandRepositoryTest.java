package elite.intel.ai.brain.actions.customcommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomCommandRepositoryTest {

    @TempDir
    Path tempDir;

    private final CustomCommandRepository repo = new CustomCommandRepository();

    private Path customCommandsFile() {
        return tempDir.resolve("custom_commands.json");
    }

    // --- missing / empty file ---

    @Test
    void missingFileReturnsEmptyList() {
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertTrue(result.isEmpty());
    }

    @Test
    void emptyFileReturnsEmptyList() throws IOException {
        Files.writeString(customCommandsFile(), "", StandardCharsets.UTF_8);
        assertTrue(repo.load(customCommandsFile()).isEmpty());
    }

    @Test
    void blankWhitespaceFileReturnsEmptyList() throws IOException {
        Files.writeString(customCommandsFile(), "   \n  ", StandardCharsets.UTF_8);
        assertTrue(repo.load(customCommandsFile()).isEmpty());
    }

    // --- malformed JSON ---

    @Test
    void malformedJsonReturnsEmptyListWithoutException() throws IOException {
        Files.writeString(customCommandsFile(), "{not valid json at all", StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> repo.load(customCommandsFile()));
        assertTrue(repo.load(customCommandsFile()).isEmpty());
    }

    @Test
    void truncatedJsonReturnsEmptyListWithoutException() throws IOException {
        Files.writeString(customCommandsFile(), "[{\"id\":\"x\",\"name\":\"", StandardCharsets.UTF_8);
        assertDoesNotThrow(() -> repo.load(customCommandsFile()));
    }

    @Test
    void jsonObjectInsteadOfArrayReturnsEmptyList() throws IOException {
        Files.writeString(customCommandsFile(), "{\"id\":\"x\"}", StandardCharsets.UTF_8);
        assertTrue(repo.load(customCommandsFile()).isEmpty());
    }

    // --- invalid custom command entries skipped ---

    @Test
    void customCommandWithBlankIdIsSkipped() throws IOException {
        writeJson("""
                [
                  {"id":"","actionKey":"custom_command_bad_id","name":"Bad","phrases":"p","steps":[{"type":"SPEAK","text":"x"}]},
                  {"id":"custom_command_good","actionKey":"custom_command_good","name":"Good","phrases":"good","steps":[{"type":"SPEAK","text":"ok"}]}
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
        assertEquals("custom_command_good", result.getFirst().getId());
    }

    @Test
    void customCommandWithBlankNameIsSkipped() throws IOException {
        writeJson("""
                [
                  {"id":"custom_command_bad","actionKey":"custom_command_bad_name","name":"","phrases":"p","steps":[{"type":"SPEAK","text":"x"}]},
                  {"id":"custom_command_ok","actionKey":"custom_command_ok_step","name":"OK","phrases":"ok","steps":[{"type":"SPEAK","text":"ok"}]}
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
        assertEquals("custom_command_ok", result.getFirst().getId());
    }

    @Test
    void customCommandWithBlankPhrasesIsSkipped() throws IOException {
        writeJson("""
                [
                  {"id":"custom_command_bad","actionKey":"custom_command_bad_phrases","name":"Bad","phrases":"","steps":[{"type":"SPEAK","text":"x"}]},
                  {"id":"custom_command_ok","actionKey":"custom_command_ok_step","name":"OK","phrases":"ok phrase","steps":[{"type":"SPEAK","text":"ok"}]}
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
    }

    @Test
    void customCommandWithEmptyStepsIsSkipped() throws IOException {
        writeJson("""
                [
                  {"id":"custom_command_bad","actionKey":"custom_command_bad_steps","name":"Bad","phrases":"p","steps":[]},
                  {"id":"custom_command_ok","actionKey":"custom_command_ok_step","name":"OK","phrases":"ok","steps":[{"type":"SPEAK","text":"ok"}]}
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
    }

    @Test
    void bindingTapStepWithoutBindingIdIsSkipped() throws IOException {
        writeJson("""
                [
                  {"id":"custom_command_bad","actionKey":"custom_command_bad_binding","name":"Bad","phrases":"p","steps":[{"type":"BINDING_TAP"}]},
                  {"id":"custom_command_ok","actionKey":"custom_command_ok_step","name":"OK","phrases":"ok","steps":[{"type":"SPEAK","text":"x"}]}
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
        assertEquals("custom_command_ok", result.getFirst().getId());
    }

    // --- valid customCommand loaded correctly ---

    @Test
    void validSingleCustomCommandIsLoaded() throws IOException {
        writeJson("""
                [
                  {
                    "id": "custom_command_test",
                    "actionKey": "custom_command_test",
                    "name": "Test Custom Command",
                    "description": "A test",
                    "phrases": "test, run test",
                    "steps": [
                      {"type": "BINDING_TAP", "bindingId": "MyBinding"},
                      {"type": "DELAY", "durationMs": 200},
                      {"type": "SPEAK", "text": "Done"}
                    ]
                  }
                ]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
        CustomCommandDefinition m = result.getFirst();
        assertEquals("custom_command_test", m.getId());
        assertEquals("Test Custom Command", m.getName());
        assertEquals("A test", m.getDescription());
        assertEquals("test, run test", m.getPhrases());
        assertEquals(3, m.getSteps().size());
        assertEquals(CustomCommandStep.Type.BINDING_TAP, m.getSteps().get(0).getType());
        assertEquals("MyBinding", m.getSteps().get(0).getBindingId());
        assertEquals(CustomCommandStep.Type.DELAY, m.getSteps().get(1).getType());
        assertEquals(200, m.getSteps().get(1).getDurationMs());
        assertEquals(CustomCommandStep.Type.SPEAK, m.getSteps().get(2).getType());
        assertEquals("Done", m.getSteps().get(2).getText());
    }

    @Test
    void resultIsUnmodifiable() throws IOException {
        writeJson("""
                [{"id":"custom_command_x","actionKey":"custom_command_x_item","name":"X","phrases":"p","steps":[{"type":"SPEAK","text":"ok"}]}]
                """);
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }

    // --- save + load roundtrip ---

    @Test
    void saveAndLoadRoundtripPreservesAllFields() {
        List<CustomCommandDefinition> original = repo.load(jsonWithOneCustomCommand());

        assertTrue(repo.save(original, customCommandsFile()));
        List<CustomCommandDefinition> reloaded = repo.load(customCommandsFile());

        assertEquals(1, reloaded.size());
        CustomCommandDefinition m = reloaded.getFirst();
        assertEquals("custom_command_roundtrip", m.getId());
        assertEquals("Roundtrip", m.getName());
        assertEquals("desc", m.getDescription());
        assertEquals("do roundtrip", m.getPhrases());
        assertEquals(1, m.getSteps().size());
        assertEquals(CustomCommandStep.Type.SPEAK, m.getSteps().getFirst().getType());
        assertEquals("hi", m.getSteps().getFirst().getText());
    }

    @Test
    void saveCreatesParentDirectories() {
        Path nestedFile = tempDir.resolve("nested").resolve("custom-commands").resolve("custom_commands.json");
        CustomCommandDefinition customCommand = new CustomCommandDefinition(
                "custom_command_nested_save",
                "Nested Save",
                "",
                "nested save",
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "hi", null))
        );

        assertTrue(repo.save(List.of(customCommand), nestedFile));

        assertTrue(Files.exists(nestedFile));
        assertEquals(1, repo.load(nestedFile).size());
    }

    // --- backup fallback on corrupt main file ---

    @Test
    void loadFallsBackToBackupWhenMainFileIsCorrupt() throws IOException {
        Files.writeString(customCommandsFile(), "not-valid-json", StandardCharsets.UTF_8);
        Files.writeString(backupFile(), validOneCustomCommandJson("custom_command_backup", "From Backup"),
                StandardCharsets.UTF_8);

        List<CustomCommandDefinition> result = repo.load(customCommandsFile());

        assertEquals(1, result.size());
        assertEquals("custom_command_backup", result.getFirst().getId());
    }

    @Test
    void loadReturnsEmptyWhenMainIsCorruptAndNoBackupExists() throws IOException {
        writeJson("""
                [
                  {"id":"custom_command_bad","actionKey":"custom_command_bad_reset","name":"","phrases":"p","steps":[{"type":"SPEAK","text":"x"}]}
                ]
                """);
        repo.load(customCommandsFile());
        assertEquals(1, repo.getLastSkippedCount());

        Files.writeString(customCommandsFile(), "not-valid-json", StandardCharsets.UTF_8);

        List<CustomCommandDefinition> result = repo.load(customCommandsFile());

        assertTrue(result.isEmpty());
        assertEquals(0, repo.getLastSkippedCount());
        assertTrue(repo.getLastSkippedLabels().isEmpty());
    }

    @Test
    void loadReturnsEmptyWhenBothMainAndBackupAreCorrupt() throws IOException {
        Files.writeString(customCommandsFile(), "not-valid-json", StandardCharsets.UTF_8);
        Files.writeString(backupFile(), "also-not-valid", StandardCharsets.UTF_8);

        List<CustomCommandDefinition> result = repo.load(customCommandsFile());

        assertTrue(result.isEmpty());
    }

    @Test
    void loadDoesNotCheckBackupWhenMainFileIsMissing() throws IOException {
        Files.writeString(backupFile(), validOneCustomCommandJson("custom_command_bak_item", "Bak Only"),
                StandardCharsets.UTF_8);

        // Main file does not exist, so backup should not be consulted.
        List<CustomCommandDefinition> result = repo.load(customCommandsFile());

        assertTrue(result.isEmpty());
    }

    // --- safe save: backup + temp + atomic rename ---

    @Test
    void saveCreatesBackupOfPreviousFile() throws IOException {
        repo.save(List.of(makeCustomCommand("custom_command_first", "First")), customCommandsFile());
        repo.save(List.of(makeCustomCommand("custom_command_second", "Second")), customCommandsFile());

        assertTrue(Files.exists(backupFile()), "Backup file should exist after second save");
        List<CustomCommandDefinition> fromBackup = repo.load(backupFile());
        assertEquals(1, fromBackup.size());
        assertEquals("custom_command_first", fromBackup.getFirst().getId());
    }

    @Test
    void saveDoesNotCreateBackupOnFirstSave() {
        assertFalse(Files.exists(backupFile()), "No backup before first save");
        repo.save(List.of(makeCustomCommand("custom_command_init", "Init")), customCommandsFile());
        assertFalse(Files.exists(backupFile()), "No backup created when there was no previous file");
    }

    @Test
    void saveTempFileIsRemovedAfterSuccessfulSave() {
        repo.save(List.of(makeCustomCommand("custom_command_tmp_file", "Temp")), customCommandsFile());

        Path tmp = customCommandsFile().resolveSibling("custom_commands.json.tmp");
        assertFalse(Files.exists(tmp), "Temp file should not remain after a successful save");
    }

    @Test
    void savedFileIsReadableAfterSave() {
        CustomCommandDefinition customCommand = makeCustomCommand("custom_command_persisted", "Persisted");
        repo.save(List.of(customCommand), customCommandsFile());

        List<CustomCommandDefinition> result = repo.load(customCommandsFile());
        assertEquals(1, result.size());
        assertEquals("custom_command_persisted", result.getFirst().getId());
    }

    // --- helpers ---

    private void writeJson(String json) throws IOException {
        Files.writeString(customCommandsFile(), json, StandardCharsets.UTF_8);
    }

    private Path backupFile() {
        return customCommandsFile().resolveSibling("custom_commands.json.bak");
    }

    private Path jsonWithOneCustomCommand() {
        try {
            writeJson("""
                    [{"id":"custom_command_roundtrip","actionKey":"custom_command_roundtrip","name":"Roundtrip","description":"desc",
                      "phrases":"do roundtrip","steps":[{"type":"SPEAK","text":"hi"}]}]
                    """);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return customCommandsFile();
    }

    /** Produces a minimal valid single-customCommand JSON array with the given {@code id} used as actionKey. */
    private static String validOneCustomCommandJson(String id, String name) {
        return "[{\"id\":\"" + id + "\",\"actionKey\":\"" + id + "\","
                + "\"name\":\"" + name + "\","
                + "\"phrases\":\"trigger\",\"steps\":[{\"type\":\"SPEAK\",\"text\":\"ok\"}]}]";
    }

    private static CustomCommandDefinition makeCustomCommand(String id, String name) {
        return new CustomCommandDefinition(id, name, "", "trigger " + id,
                List.of(new CustomCommandStep(CustomCommandStep.Type.SPEAK, null, 0, "ok", null)));
    }
}
