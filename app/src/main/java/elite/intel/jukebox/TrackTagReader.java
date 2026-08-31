package elite.intel.jukebox;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads what a music file says about itself. A seam, so the scanner's behaviour - batching, failure
 * handling, marking a file read - can be tested without any real audio on disk.
 */
@FunctionalInterface
public interface TrackTagReader {

    /**
     * @throws IOException when the file cannot be opened or parsed at all
     */
    TrackTags read(Path file) throws IOException;
}
