package elite.intel.jukebox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reading the playlists other music players export.
 *
 * <p>The awkward parts of M3U are all here: relative paths that only mean something next to the playlist
 * file, {@code #EXTINF} lines that look like entries but are not, plain {@code .m3u} written in whatever
 * character set the machine that made it used, and entries naming things this build cannot play.
 */
class PlaylistFileImporterTest {

    @Test
    void readsOnePathPerLineInOrder(@TempDir Path folder) throws IOException {
        music(folder, "one.mp3", "two.mp3", "three.mp3");
        Path playlist = write(folder, "list.m3u", "one.mp3\ntwo.mp3\nthree.mp3\n");

        List<String> tracks = PlaylistFileImporter.read(playlist);

        assertEquals(3, tracks.size());
        assertTrue(tracks.get(0).endsWith("one.mp3"));
        assertTrue(tracks.get(2).endsWith("three.mp3"), "a playlist's order is the point of it");
    }

    @Test
    void relativePathsResolveAgainstThePlaylistNotTheWorkingDirectory(@TempDir Path folder) throws IOException {
        music(folder.resolve("Album"), "track.mp3");
        Path playlist = write(folder, "list.m3u", "Album/track.mp3\n");

        List<String> tracks = PlaylistFileImporter.read(playlist);

        assertEquals(1, tracks.size());
        assertEquals(folder.resolve("Album/track.mp3").toAbsolutePath().normalize().toString(),
                tracks.get(0), "this is what lets a playlist travel with the music it names");
    }

    @Test
    void extendedInformationLinesAreNotMistakenForTracks(@TempDir Path folder) throws IOException {
        music(folder, "song.mp3");
        Path playlist = write(folder, "list.m3u",
                "#EXTM3U\n#EXTINF:245,Stellar Cartography - Aphelion Drift\nsong.mp3\n");

        assertEquals(1, PlaylistFileImporter.read(playlist).size(),
                "everything after a hash is commentary, however much it looks like data");
    }

    @Test
    void streamsAreSkippedBecauseThisPlaysFiles(@TempDir Path folder) throws IOException {
        music(folder, "local.mp3");
        Path playlist = write(folder, "list.m3u",
                "http://example.com/stream.mp3\nlocal.mp3\nhttps://example.com/other.mp3\n");

        List<String> tracks = PlaylistFileImporter.read(playlist);

        assertEquals(1, tracks.size());
        assertTrue(tracks.get(0).endsWith("local.mp3"));
    }

    @Test
    void formatsThisBuildCannotPlayAreLeftOut(@TempDir Path folder) throws IOException {
        music(folder, "playable.mp3");
        Path playlist = write(folder, "list.m3u", "playable.mp3\nlossless.flac\nvideo.mkv\n");

        assertEquals(1, PlaylistFileImporter.read(playlist).size(),
                "adding a FLAC would flag it missing a moment later, which reads as a bug");
    }

    @Test
    void theSameTrackNamedTwiceIsAddedOnce(@TempDir Path folder) throws IOException {
        music(folder, "song.mp3");
        Path playlist = write(folder, "list.m3u", "song.mp3\nsong.mp3\n");

        assertEquals(1, PlaylistFileImporter.read(playlist).size());
    }

    @Test
    void aLatinOnePlaylistIsReadRatherThanRejected(@TempDir Path folder) throws IOException {
        music(folder, "bjork.mp3");
        Path playlist = folder.resolve("legacy.m3u");
        // A plain .m3u predates the UTF-8 convention: this one was written by a Latin-1 machine, and the
        // accented byte is invalid UTF-8. Read strictly it would throw and lose the whole playlist.
        Files.write(playlist, "#EXTINF:1,Björk\nbjork.mp3\n".getBytes(StandardCharsets.ISO_8859_1));

        assertEquals(1, PlaylistFileImporter.read(playlist).size());
    }

    @Test
    void blankLinesAreIgnored(@TempDir Path folder) throws IOException {
        music(folder, "song.mp3");
        Path playlist = write(folder, "list.m3u", "\n\nsong.mp3\n\n   \n");

        assertEquals(1, PlaylistFileImporter.read(playlist).size());
    }

    @Test
    void aPlaylistThatIsNotThereIsReported(@TempDir Path folder) {
        assertThrows(IOException.class, () -> PlaylistFileImporter.read(folder.resolve("gone.m3u")));
    }

    @Test
    void bothExtensionsAreRecognisedAndNothingElseIs() {
        assertTrue(PlaylistFileImporter.isPlaylist(Path.of("a.m3u")));
        assertTrue(PlaylistFileImporter.isPlaylist(Path.of("a.m3u8")));
        assertTrue(PlaylistFileImporter.isPlaylist(Path.of("A.M3U")));
        assertFalse(PlaylistFileImporter.isPlaylist(Path.of("a.pls")),
                "PLS is deliberately not read - nothing exports it that does not also export M3U");
        assertFalse(PlaylistFileImporter.isPlaylist(Path.of("a.mp3")));
    }

    private static Path write(Path folder, String name, String body) throws IOException {
        Files.createDirectories(folder);
        Path file = folder.resolve(name);
        Files.writeString(file, body, StandardCharsets.UTF_8);
        return file;
    }

    private static void music(Path folder, String... names) throws IOException {
        Files.createDirectories(folder);
        for (String name : names) {
            Files.writeString(folder.resolve(name), "audio");
        }
    }
}
