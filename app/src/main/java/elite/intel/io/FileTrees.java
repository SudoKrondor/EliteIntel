package elite.intel.io;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Deletes a folder with everything in it, knowing nothing about what the folder holds.
 */
public final class FileTrees {

    private FileTrees() {
    }

    /**
     * Deletes {@code root} and everything under it, deepest first. A symbolic link is deleted as a link, never followed.
     * Nothing happens when {@code root} does not exist.
     * <p>
     * WHY the read-only retry: on Windows a file marked read-only refuses to be deleted, and installers mark some of
     * theirs that way. Clearing the mark is what Explorer and {@code Remove-Item -Force} do too.
     */
    public static void delete(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        List<Path> deepestFirst;
        try (Stream<Path> walk = Files.walk(root)) {
            deepestFirst = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : deepestFirst) {
            try {
                Files.delete(path);
            } catch (AccessDeniedException readOnly) {
                if (!clearReadOnly(path)) throw readOnly;
                Files.delete(path);
            }
        }
    }

    private static boolean clearReadOnly(Path path) {
        try {
            Files.setAttribute(path, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS);
            return true;
        } catch (IOException | UnsupportedOperationException noDosAttributes) {
            return false;
        }
    }
}
