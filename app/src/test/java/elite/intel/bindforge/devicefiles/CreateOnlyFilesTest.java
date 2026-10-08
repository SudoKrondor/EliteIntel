package elite.intel.bindforge.devicefiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The create-only write: a new file appears whole, an existing one is never replaced, and no temp file is left. */
class CreateOnlyFilesTest {

    @TempDir
    Path temp;

    @Test
    void aNewFileIsWrittenAndItsFolderCreated() throws IOException {
        Path target = temp.resolve("DeviceButtonMaps").resolve("Mine.buttonMap");

        assertTrue(CreateOnlyFiles.createNew(target, bytes("first")));

        assertEquals("first", Files.readString(target, StandardCharsets.UTF_8));
        assertEquals(List.of(target), listing(target.getParent()));
    }

    @Test
    void anExistingFileIsNeverReplacedAndNoTempFileIsLeft() throws IOException {
        Path target = temp.resolve("Mine.buttonMap");
        Files.writeString(target, "theirs");

        assertFalse(CreateOnlyFiles.createNew(target, bytes("ours")));

        assertEquals("theirs", Files.readString(target, StandardCharsets.UTF_8));
        assertEquals(List.of(target), listing(temp));
    }

    /** An interrupted write - the service stopping - fails, leaves no target and no temp file in the folder. */
    @Test
    void anInterruptedWriteLeavesNothingBehind() {
        Path target = temp.resolve("Mine.buttonMap");

        Thread.currentThread().interrupt();
        try {
            CreateOnlyFiles.createNew(target, bytes("ours"));
        } catch (IOException expected) {
            // ClosedByInterruptException from the flush
        } finally {
            // Clears the flag, so it cannot leak into the next test on this thread; whether it was still set is moot.
            Thread.interrupted();
        }

        assertFalse(Files.exists(target));
        assertEquals(List.of(), listing(temp));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static List<Path> listing(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
