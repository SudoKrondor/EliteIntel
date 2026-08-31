package elite.intel.jukebox;

/**
 * Where the jukebox's audio goes. A seam, so the transport can be tested without a sound card - the build
 * server has none, and a test that needs one is a test that only runs on a developer's desktop.
 */
public interface MusicOutput extends AutoCloseable {

    /**
     * Opens the device. The format is always {@link MusicFormat#CANONICAL}.
     */
    void open() throws Exception;

    /**
     * Writes audio, blocking until the device has room. This is what paces playback.
     */
    void write(byte[] pcm, int offset, int length);

    /**
     * Discards audio already queued but not yet heard, so a stop or pause is immediate.
     */
    void flush();

    @Override
    void close();
}
