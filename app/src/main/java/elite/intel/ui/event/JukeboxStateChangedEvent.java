package elite.intel.ui.event;

import elite.intel.jukebox.PlaybackOrder;
import elite.intel.jukebox.PlaybackState;

/**
 * Published on the UI bus when the jukebox starts, stops, pauses, moves to another track, or is told to
 * play in a different order, so the playlist can highlight what is playing and the transport buttons and
 * the order selector can show the right state.
 * <p>
 * Deliberately not published as the position advances: that would be hundreds of events a second to say
 * something a progress display can read when it repaints.
 * <p>
 * WHY the order rides along rather than getting an event of its own: a commander saying "randomise the
 * playlist" changes the player, not the tab, and the tab has no other way to hear about it. One event
 * carrying the whole of what the transport row shows keeps the tab from having to reconcile two.
 */
public class JukeboxStateChangedEvent {

    private final PlaybackState state;
    private final Long trackId;
    private final PlaybackOrder order;

    public JukeboxStateChangedEvent(PlaybackState state, Long trackId, PlaybackOrder order) {
        this.state = state;
        this.trackId = trackId;
        this.order = order;
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

    /**
     * The order the jukebox will pick the next track in.
     */
    public PlaybackOrder getOrder() {
        return order;
    }
}
