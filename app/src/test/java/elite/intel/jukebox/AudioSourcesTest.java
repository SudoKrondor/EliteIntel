package elite.intel.jukebox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Choosing a decoder by extension.
 *
 * <p>The point of the class under test is that one answer serves both the library scan and playback, so
 * these check the two together: whatever {@code isPlayable} admits must be something {@code open} can
 * actually open. A disagreement there is invisible until a track's turn comes and it is marked missing.
 */
class AudioSourcesTest {

    @Test
    void everyExtensionTheScannerAdmitsCanActuallyBeOpened() throws Exception {
        // The guard on the whole design: if a format is ever added to the map without a working decoder,
        // the library would import files that fail hours later, when the track's turn comes.
        for (String extension : AudioSources.extensions()) {
            String fixture = "tone-44100-stereo" + extension;
            assertTrue(AudioSources.isPlayable(Path.of(fixture)),
                    extension + " is in the map but not admitted by isPlayable");
            try (AudioSource source = AudioSources.open(fixturePath(fixture), 0)) {
                assertTrue(source.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES) > 0,
                        extension + " was admitted by the scanner but decoded no audio");
            }
        }
    }

    @Test
    void everyFormatIsRecognisedWhateverTheCase() {
        assertTrue(AudioSources.isPlayable(Path.of("song.mp3")));
        assertTrue(AudioSources.isPlayable(Path.of("song.flac")));
        assertTrue(AudioSources.isPlayable(Path.of("song.m4a")));
        assertTrue(AudioSources.isPlayable(Path.of("book.m4b")), "audiobooks arrive as .m4b");
        assertTrue(AudioSources.isPlayable(Path.of("song.ogg")));
        assertTrue(AudioSources.isPlayable(Path.of("song.oga")), "Xiph's own audio extension");
        assertTrue(AudioSources.isPlayable(Path.of("RIPPED.FLAC")),
                "a file ripped on Windows may well be named .FLAC and is no less playable for it");
    }

    @Test
    void formatsWithNoDecoderAreNotAdmitted() {
        assertFalse(AudioSources.isPlayable(Path.of("podcast.wma")));
        assertFalse(AudioSources.isPlayable(Path.of("cover.jpg")));
        assertFalse(AudioSources.isPlayable(Path.of("list.m3u")));
        assertFalse(AudioSources.isPlayable(null));
    }

    @Test
    void openingAFormatWeDoNotPlayFailsRatherThanGuessingADecoder() {
        assertThrows(IOException.class, () -> AudioSources.open(Path.of("/music/podcast.wma"), 0));
    }

    @Test
    void theExtensionChoosesTheDecoderRatherThanTheContent() throws Exception {
        // Both fixtures are the same tone, so only the dispatch distinguishes them: pointed at the FLAC
        // the MP3 decoder would fail, and vice versa.
        try (AudioSource mp3 = AudioSources.open(fixturePath("tone-44100-stereo.mp3"), 0);
             AudioSource flac = AudioSources.open(fixturePath("tone-44100-stereo.flac"), 0)) {
            assertTrue(mp3.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES) > 0);
            assertTrue(flac.read(new byte[MusicFormat.BLOCK_BYTES], 0, MusicFormat.BLOCK_BYTES) > 0);
        }
    }

    private static Path fixturePath(String name) throws URISyntaxException {
        var url = AudioSourcesTest.class.getResource("/jukebox/" + name);
        assertNotNull(url, "missing test fixture: " + name);
        return Path.of(url.toURI());
    }
}
