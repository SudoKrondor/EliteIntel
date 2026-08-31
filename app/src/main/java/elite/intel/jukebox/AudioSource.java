package elite.intel.jukebox;

import java.io.IOException;

/**
 * A track being read, delivering audio already in {@link MusicFormat#CANONICAL} form.
 * <p>
 * The player is written against this rather than against a decoder so that what it does - transport,
 * ducking, gain, advancing the playlist - can be exercised without a codec or a sound card, and so a
 * second container format later is a new implementation rather than a change to the player.
 */
public interface AudioSource extends AutoCloseable {

    /**
     * Reads the next stretch of audio.
     *
     * @return how many bytes were read, or -1 at the end of the track
     */
    int read(byte[] buffer, int offset, int length) throws IOException;

    /**
     * How far into the track the reader has got, in milliseconds.
     */
    long positionMs();

    @Override
    void close();
}
