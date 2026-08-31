package elite.intel.jukebox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Finds the playable music under a folder the commander picked.
 * <p>
 * Searches sub-folders too, because a music library is almost always arranged as artist and album
 * directories and a commander who points at the top of one means all of it. Results come back in path
 * order, so albums arrive as albums and an audiobook's chapters arrive in the order they are numbered
 * rather than in whatever order the file system happened to hand them over.
 */
public final class MusicFolderScanner {

    /**
     * How deep the search goes. Deep enough for artist/album/disc, shallow enough that pointing at a home
     * directory by mistake does not walk an entire drive.
     */
    private static final int MAX_DEPTH = 8;

    private static final String EXTENSION = ".mp3";

    private MusicFolderScanner() {
    }

    /**
     * Every MP3 under {@code root}, in path order.
     *
     * @throws IOException when the folder cannot be read at all - an unmounted drive, or one the commander
     *                     has no permission for. A folder containing no music is not an error, it is an
     *                     empty list.
     */
    public static List<String> findTracks(Path root) throws IOException {
        if (root == null || !Files.isDirectory(root)) {
            throw new IOException("Not a readable folder: " + root);
        }
        List<String> found = new ArrayList<>();
        // Symbolic links are not followed: a link pointing back up its own tree would otherwise walk in
        // circles, and a link into another library would import it without the commander asking.
        try (Stream<Path> walk = Files.walk(root, MAX_DEPTH)) {
            walk.filter(Files::isRegularFile)
                    .filter(MusicFolderScanner::isPlayable)
                    .map(path -> path.toAbsolutePath().toString())
                    .forEach(found::add);
        }
        found.sort(Comparator.naturalOrder());
        return found;
    }

    private static boolean isPlayable(Path path) {
        Path name = path.getFileName();
        return name != null && name.toString().toLowerCase(Locale.ROOT).endsWith(EXTENSION);
    }
}
