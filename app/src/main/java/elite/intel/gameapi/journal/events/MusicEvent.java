package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * The game's soundtrack cue: written every time the music track changes, whether or not the commander has
 * the music turned up (a muted client still reports {@code NoInGameMusic} and the cues around it).
 * <p>
 * Kept for one reason: it is the only signal the game gives for the galaxy map opening while the commander
 * is on foot. Status.json drops {@code GuiFocus} entirely outside a vehicle, so on foot the map can only be
 * seen through the {@code GalaxyMap} track starting and stopping.
 */
public class MusicEvent extends BaseEvent {

    /**
     * The track the game reports while the galaxy map is up.
     */
    public static final String GALAXY_MAP_TRACK = "GalaxyMap";

    @SerializedName("MusicTrack")
    public String musicTrack;

    public MusicEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Music");
        MusicEvent event = GsonFactory.getGson().fromJson(json, MusicEvent.class);
        this.musicTrack = event.musicTrack;
    }

    @Override
    public String getEventType() {
        return "Music";
    }

    /**
     * Soundtrack cues are constant background noise; nothing for VEGA to remember.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "The in-game music track changed.";
    }

    public String getMusicTrack() {
        return musicTrack;
    }

    /**
     * Whether this cue is the galaxy map's own track, i.e. the map has just opened.
     */
    public boolean isGalaxyMapTrack() {
        return GALAXY_MAP_TRACK.equals(musicTrack);
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }
}
