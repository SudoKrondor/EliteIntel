package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import elite.intel.gameapi.journal.EventRegistry;
import elite.intel.util.json.GsonFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The soundtrack cue is the only sign of the galaxy map opening on foot, so the registry has to know it and
 * the map's own track has to be told apart from every other cue. Lines copied from a commander's journal.
 */
class MusicEventTest {

    @Test
    void theRegistryBuildsAMusicEvent() {
        BaseEvent event = EventRegistry.createEvent("Music", line("GalaxyMap"));
        assertInstanceOf(MusicEvent.class, event);
        assertEquals("GalaxyMap", ((MusicEvent) event).getMusicTrack());
    }

    @Test
    void onlyTheGalaxyMapTrackMeansTheMapIsOpen() {
        assertTrue(new MusicEvent(line("GalaxyMap")).isGalaxyMapTrack());
        // What a muted client reports once the map is shut, and what a playing one reports.
        assertFalse(new MusicEvent(line("NoInGameMusic")).isGalaxyMapTrack());
        assertFalse(new MusicEvent(line("NoTrack")).isGalaxyMapTrack());
        assertFalse(new MusicEvent(line("Exploration")).isGalaxyMapTrack());
        assertFalse(new MusicEvent(line("FleetCarrier_Managment")).isGalaxyMapTrack());
    }

    private static JsonObject line(String track) {
        return GsonFactory.getGson().fromJson(
                "{ \"timestamp\":\"" + Instant.now() + "\", \"event\":\"Music\", \"MusicTrack\":\"" + track + "\" }",
                JsonObject.class);
    }
}
