package elite.intel.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicFilesTest {

    @TempDir
    Path dir;

    @Test
    void writeCreatesAMissingFile() throws IOException {
        Path target = dir.resolve("settings.json");

        AtomicFiles.write(target, bytes("{}"));

        assertEquals("{}", Files.readString(target));
    }

    @Test
    void writeReplacesExistingContentAndLeavesNoTempFileBehind() throws IOException {
        Path target = dir.resolve("Custom.4.2.binds");
        Files.writeString(target, "old content that is longer than the new");

        AtomicFiles.write(target, bytes("new"));

        assertEquals("new", Files.readString(target));
        assertEquals(List.of(target), entries());
    }

    @Test
    void failedReplaceRemovesItsTempFile() throws IOException {
        Path target = dir.resolve("occupied");
        Files.createDirectories(target.resolve("child"));

        assertThrows(IOException.class, () -> AtomicFiles.write(target, bytes("new")));

        assertEquals(List.of(target), entries());
    }

    @Test
    void copyReplacesTargetAndKeepsTheSourceModifiedTime() throws IOException {
        Path source = dir.resolve("game.binds");
        Path target = dir.resolve("working.binds");
        Files.writeString(source, "game");
        Files.writeString(target, "stale");
        FileTime gameTime = FileTime.from(Instant.parse("2026-06-24T18:00:00Z"));
        Files.setLastModifiedTime(source, gameTime);

        AtomicFiles.copy(source, target);

        assertEquals("game", Files.readString(target));
        assertEquals(gameTime, Files.getLastModifiedTime(target));
        assertEquals(List.of(source, target), entries());
    }

    private List<Path> entries() throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.sorted().toList();
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
