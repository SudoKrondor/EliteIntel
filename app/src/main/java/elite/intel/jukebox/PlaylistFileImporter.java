package elite.intel.jukebox;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Reads an M3U or M3U8 playlist into a list of files to add.
 * <p>
 * M3U is the format every music player exports, and its whole specification is: one path per line, lines
 * beginning {@code #} are comments, and {@code #EXTINF} carries a duration and title for the line that
 * follows. That metadata is deliberately ignored here - the tag scanner reads the files themselves, which
 * is right more often than a playlist written years ago by another program.
 * <p>
 * PLS is not read. It says the same thing in an INI dialect, and nothing exports it that does not also
 * export M3U.
 */
public final class PlaylistFileImporter {

    private static final String COMMENT_PREFIX = "#";

    private PlaylistFileImporter() {
    }

    /**
     * True for a file this can read, by extension.
     */
    public static boolean isPlaylist(Path file) {
        if (file == null || file.getFileName() == null) return false;
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".m3u") || name.endsWith(".m3u8");
    }

    /**
     * The playable files a playlist names, in its order, as absolute paths.
     * <p>
     * Entries are resolved against the playlist's own folder, which is how relative paths in an M3U are
     * meant to be read and what makes a playlist that travels with its music still work. Remote entries
     * are skipped: this plays files, not streams. Anything there is no decoder for is skipped too - it would
     * otherwise be added and then immediately flagged as missing, which reads as a bug rather than as the
     * "cannot play this yet" it actually is.
     *
     * @throws IOException when the playlist itself cannot be read
     */
    public static List<String> read(Path playlist) throws IOException {
        if (playlist == null || !Files.isRegularFile(playlist)) {
            throw new IOException("Not a readable playlist: " + playlist);
        }
        Path folder = playlist.toAbsolutePath().getParent();
        List<String> entries = new ArrayList<>(new LinkedHashSet<>(lines(playlist)));
        List<String> tracks = new ArrayList<>();
        for (String line : entries) {
            String entry = line.trim();
            if (entry.isEmpty() || entry.startsWith(COMMENT_PREFIX) || isRemote(entry)) continue;
            Path resolved = resolve(folder, entry);
            if (resolved == null || !AudioSources.isPlayable(resolved)) continue;
            tracks.add(resolved.toString());
        }
        return tracks;
    }

    /**
     * WHY two character sets: {@code .m3u8} is UTF-8 by definition, but plain {@code .m3u} predates that
     * and is whatever the machine that wrote it used. Reading a Latin-1 playlist strictly as UTF-8 throws
     * on the first accented artist name, so a failure falls back rather than losing the whole file.
     */
    private static List<String> lines(Path playlist) throws IOException {
        try {
            return Files.readAllLines(playlist, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readAllLines(playlist, StandardCharsets.ISO_8859_1);
        }
    }

    private static boolean isRemote(String entry) {
        String lower = entry.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("mms://") || lower.startsWith("rtsp://");
    }

    private static Path resolve(Path folder, String entry) {
        try {
            Path candidate = Path.of(entry);
            Path absolute = candidate.isAbsolute() || folder == null
                    ? candidate
                    : folder.resolve(candidate);
            return absolute.normalize();
        } catch (InvalidPathException e) {
            // A playlist written on another operating system can name paths this one cannot express.
            return null;
        }
    }
}
