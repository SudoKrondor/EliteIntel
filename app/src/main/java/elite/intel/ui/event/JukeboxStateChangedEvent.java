package elite.intel.ui.event;

import elite.intel.jukebox.PlaybackState;

/**
 * Published on the UI bus when the jukebox starts, stops, pauses, or moves to another track, so the
 * playlist can highlight what is playing and the transport buttons can show the right state.
 * <p>
 * Deliberately not published as the position advances: that would be hundreds of events a second to say
 * something a progress display can read when it repaints.
 */
public class JukeboxStateChangedEvent {

    private final PlaybackState state;
    private final Long trackId;

    public JukeboxStateChangedEvent(PlaybackState state, Long trackId) {
        this.state = state;
        this.trackId = trackId;
    }

    public PlaybackState getState() {
        return state;
    }

    /**
     * The track playing or paused, or null when the jukebox is stopped.
     */
    public Long getTrackId() {
        return trackId;
    }
}
