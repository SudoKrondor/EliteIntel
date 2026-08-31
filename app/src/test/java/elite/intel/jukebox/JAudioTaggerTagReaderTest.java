package elite.intel.jukebox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reading what a file says about itself, against real MP3s rather than a mock - tag parsing is exactly the
 * kind of thing that passes against a stub and fails against a file some other program wrote.
 */
class JAudioTaggerTagReaderTest {

    private final TrackTagReader reader = new JAudioTaggerTagReader();

    @Test
    void aTaggedFileReportsWhatItSays() throws Exception {
        TrackTags tags = reader.read(fixture("tagged.mp3"));

        assertEquals("Aphelion Drift", tags.title());
        assertEquals("Stellar Cartography", tags.artist());
        assertEquals("Deep Black", tags.album());
        assertEquals(7, tags.trackNumber());
    }

    @Test
    void anUntaggedFileStillReportsItsLength() throws Exception {
        TrackTags tags = reader.read(fixture("tone-44100-stereo.mp3"));

        assertNull(tags.title(), "this fixture carries no title, and inventing one would be worse");
        assertNotNull(tags.durationMs(),
                "the duration comes from the audio itself, so it is known even with no tags at all");
        assertEquals(470, tags.durationMs(), 60);
    }

    @Test
    void theDurationIsReadFromTheAudioNotFromATag() throws Exception {
        // A tagged file and an untagged one of the same shape both answer, because neither answer came
        // from TLEN - which most files omit and many of the rest get wrong.
        assertNotNull(reader.read(fixture("tagged.mp3")).durationMs());
        assertNotNull(reader.read(fixture("tone-32000-mono.mp3")).durationMs());
    }

    @Test
    void aFileThatIsNotAudioIsReportedRatherThanGuessedAt(@TempDir Path folder) throws IOException {
        Path pretender = folder.resolve("not-really.mp3");
        Files.writeString(pretender, "this is not an MP3 at all");

        assertThrows(IOException.class, () -> reader.read(pretender),
                "a corrupt file must fail loudly here so the scanner can mark it read and move on");
    }

    @Test
    void aMissingFileIsReported(@TempDir Path folder) {
        assertThrows(IOException.class, () -> reader.read(folder.resolve("gone.mp3")));
    }

    @Test
    void theReaderIsQuiet() throws Exception {
        // jaudiotagger narrates every field it reads through java.util.logging at INFO. If that were left
        // on, scanning a library would bury the application log; the reader turns it off in a static block.
        assertTrue(java.util.logging.Logger.getLogger("org.jaudiotagger").getLevel()
                        == java.util.logging.Level.OFF,
                "tag logging must stay off or a first scan floods the log");
        reader.read(fixture("tagged.mp3"));
    }

    private static Path fixture(String name) throws URISyntaxException {
        var url = JAudioTaggerTagReaderTest.class.getResource("/jukebox/" + name);
        assertNotNull(url, "missing fixture " + name);
        return Path.of(url.toURI());
    }
}
