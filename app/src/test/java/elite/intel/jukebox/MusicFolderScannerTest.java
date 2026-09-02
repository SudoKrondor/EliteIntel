package elite.intel.jukebox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Turning a folder the commander pointed at into a playlist.
 *
 * <p>A music library is artist and album directories, so the search goes down into them; and an audiobook
 * is a numbered set of chapters, so the order it comes back in has to be the order they are named.
 */
class MusicFolderScannerTest {

    @Test
    void findsMusicInSubFoldersNotJustTheOneChosen(@TempDir Path root) throws IOException {
        write(root, "loose.mp3");
        write(root.resolve("Artist/Album"), "track.mp3");

        List<String> found = MusicFolderScanner.findTracks(root);

        assertEquals(2, found.size(),
                "pointing at the top of a library means the library, not just its top folder");
    }

    @Test
    void chaptersComeBackInTheOrderTheyAreNumbered(@TempDir Path root) throws IOException {
        write(root, "03-third.mp3");
        write(root, "01-first.mp3");
        write(root, "02-second.mp3");

        List<String> found = MusicFolderScanner.findTracks(root);

        assertTrue(found.get(0).endsWith("01-first.mp3"), "first chapter is not first");
        assertTrue(found.get(1).endsWith("02-second.mp3"));
        assertTrue(found.get(2).endsWith("03-third.mp3"),
                "an audiobook has to arrive in order, not in whatever order the disk gave it");
    }

    @Test
    void anythingThereIsNoDecoderForIsLeftAlone(@TempDir Path root) throws IOException {
        write(root, "song.mp3");
        write(root, "album.flac");
        write(root, "cover.jpg");
        write(root, "notes.txt");
        write(root, "podcast.wma");

        List<String> found = MusicFolderScanner.findTracks(root);

        assertEquals(2, found.size(),
                "the playable formats go in; the artwork, the sleeve notes and a format with no decoder do not");
    }

    @Test
    void theExtensionIsMatchedWhateverItsCase(@TempDir Path root) throws IOException {
        write(root, "shouty.MP3");
        write(root, "mixed.Mp3");
        write(root, "ripped.FLAC");

        assertEquals(3, MusicFolderScanner.findTracks(root).size(),
                "a file ripped on Windows may well be named .MP3 and is no less playable for it");
    }

    @Test
    void aFolderWithNoMusicIsAnEmptyPlaylistRatherThanAFailure(@TempDir Path root) throws IOException {
        write(root, "readme.txt");

        assertTrue(MusicFolderScanner.findTracks(root).isEmpty());
    }

    @Test
    void aFolderThatIsNotThereIsReportedRatherThanSilentlyEmpty(@TempDir Path root) {
        assertThrows(IOException.class, () -> MusicFolderScanner.findTracks(root.resolve("gone")),
                "an unmounted drive must not look like a folder that happens to hold no music");
    }

    @Test
    void pathsComeBackAbsoluteSoTheyStillResolveNextLaunch(@TempDir Path root) throws IOException {
        write(root, "song.mp3");

        assertTrue(Path.of(MusicFolderScanner.findTracks(root).get(0)).isAbsolute(),
                "the playlist outlives the working directory it was built from");
    }

    private static void write(Path folder, String name) throws IOException {
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name), "not really audio");
    }
}
