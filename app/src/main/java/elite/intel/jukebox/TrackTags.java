package elite.intel.jukebox;

/**
 * What a music file says about itself. Every field is optional except the duration, because a great many
 * files carry no tags at all while every decodable file has a length.
 *
 * @param title       the track title, or null when untagged
 * @param artist      the performer, or null
 * @param album       the release, or null
 * @param trackNumber position on the release, or null
 * @param durationMs  playing time, or null when it could not be determined
 */
public record TrackTags(String title, String artist, String album, Integer trackNumber, Long durationMs) {

    /**
     * What is known about a file nothing could be read from. Still worth storing, so it is not re-read.
     */
    public static final TrackTags UNKNOWN = new TrackTags(null, null, null, null, null);
}
