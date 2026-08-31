package elite.intel.jukebox;

/**
 * The order the jukebox picks the next track in, stored as the constant name in
 * {@code jukebox_state.playbackOrder}.
 * <p>
 * {@link #SEQUENTIAL} is the default because the playlist order is the commander's own - they dragged the
 * rows into it - and an app that shuffles a deliberately ordered list without being asked has thrown that
 * work away. It is also what an audiobook needs, and audiobooks are half of why this feature exists.
 */
public enum PlaybackOrder {
    SEQUENTIAL,
    RANDOM;

    /**
     * Resolves a stored setting value, falling back to {@link #SEQUENTIAL} for anything this build does not
     * recognise (null, blank, or an order written by a newer version).
     */
    public static PlaybackOrder fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return SEQUENTIAL;
        }
        for (PlaybackOrder order : values()) {
            if (order.name().equalsIgnoreCase(stored.trim())) {
                return order;
            }
        }
        return SEQUENTIAL;
    }
}
