package elite.intel.jukebox;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads ID3 tags and the audio header with jaudiotagger.
 * <p>
 * The duration comes from the header rather than from a {@code TLEN} tag: that tag is absent from most
 * files and wrong in a good share of the rest, whereas the header is derived from the file itself and is
 * correct for variable-bitrate encodes, which a naive bytes-over-bitrate estimate is not.
 */
final class JAudioTaggerTagReader implements TrackTagReader {

    static {
        // jaudiotagger narrates every field it reads through java.util.logging, at INFO. Left alone it
        // buries the application's own log under thousands of lines the first time a library is scanned.
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    @Override
    public TrackTags read(Path file) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(file.toFile());
            return new TrackTags(
                    field(audioFile.getTag(), FieldKey.TITLE),
                    field(audioFile.getTag(), FieldKey.ARTIST),
                    field(audioFile.getTag(), FieldKey.ALBUM),
                    trackNumber(audioFile.getTag()),
                    durationMs(audioFile.getAudioHeader()));
        } catch (Exception e) {
            // jaudiotagger throws half a dozen checked types plus runtime ones for a malformed file, and
            // every one of them means the same thing here: this file has nothing to tell us.
            throw new IOException("Could not read tags from " + file.getFileName(), e);
        }
    }

    private static String field(Tag tag, FieldKey key) {
        if (tag == null) return null;
        String value = tag.getFirst(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * The track number, from a field that is written as plainly {@code 7} by some taggers and as
     * {@code 7/12} - position and total - by others.
     */
    private static Integer trackNumber(Tag tag) {
        String raw = field(tag, FieldKey.TRACK);
        if (raw == null) return null;
        String leading = raw.split("/")[0].trim();
        try {
            return Integer.valueOf(leading);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long durationMs(AudioHeader header) {
        if (header == null) return null;
        double seconds = header.getPreciseTrackLength();
        return seconds <= 0 ? null : Math.round(seconds * 1000);
    }
}
