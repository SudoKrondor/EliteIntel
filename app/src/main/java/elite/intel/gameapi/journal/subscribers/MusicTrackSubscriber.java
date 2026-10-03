package elite.intel.gameapi.journal.subscribers;

import com.google.common.eventbus.Subscribe;
import elite.intel.gameapi.journal.events.MusicEvent;
import elite.intel.session.Status;

/**
 * Mirrors the galaxy map's soundtrack cue into {@link Status}, so the map can be seen open on foot, where
 * Status.json carries no {@code GuiFocus} at all. The track is {@code GalaxyMap} exactly while the map is up
 * and changes to something else the moment it closes, so the cue is a clean open/closed state.
 */
@SuppressWarnings("unused")
public class MusicTrackSubscriber {

    @Subscribe
    public void onMusic(MusicEvent event) {
        Status.getInstance().setGalaxyMapMusicPlaying(event.isGalaxyMapTrack());
    }
}
