package elite.intel.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTreesTest {

    @Test
    void aFolderGoesWithEverythingInIt(@TempDir Path parent) throws IOException {
        Path root = Files.createDirectories(parent.resolve("LMStudio/models/publisher/model"));
        Files.writeString(root.resolve("weights.gguf"), "x");
        Files.writeString(parent.resolve("LMStudio/.elite-intel-install"), "id");

        FileTrees.delete(parent.resolve("LMStudio"));

        assertFalse(Files.exists(parent.resolve("LMStudio")));
        assertTrue(Files.isDirectory(parent), "the folder it sat in is left alone");
    }

    @Test
    void aLinkIsDeletedWithoutFollowingIt(@TempDir Path parent) throws IOException {
        Path kept = Files.createDirectories(parent.resolve("kept"));
        Files.writeString(kept.resolve("precious.txt"), "x");
        Path root = Files.createDirectories(parent.resolve("root"));
        try {
            Files.createSymbolicLink(root.resolve("link"), kept);
        } catch (UnsupportedOperationException | IOException noLinksHere) {
            return; // WHY: Windows without developer mode cannot make links; the case does not arise there either.
        }

        FileTrees.delete(root);

        assertFalse(Files.exists(root));
        assertTrue(Files.exists(kept.resolve("precious.txt")), "what a link pointed at is never deleted through it");
    }

    @Test
    void nothingToDeleteIsNotAnError(@TempDir Path parent) {
        assertDoesNotThrow(() -> FileTrees.delete(parent.resolve("never-made")));
    }
}
