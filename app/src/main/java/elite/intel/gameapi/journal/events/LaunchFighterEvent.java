package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class LaunchFighterEvent extends BaseEvent {

    @SerializedName("Loadout")
    private String loadout;

    @SerializedName("ID")
    private int id;

    @SerializedName("PlayerControlled")
    private boolean playerControlled;

    public LaunchFighterEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "LaunchFighter");
        LaunchFighterEvent event = GsonFactory.getGson().fromJson(json, LaunchFighterEvent.class);
        this.loadout = event.loadout;
        this.id = event.id;
        this.playerControlled = event.playerControlled;
    }

    @Override
    public String getEventType() {
        return "LaunchFighter";
    }

    @Override
    public String llmDescription() {
        return "Launched a ship-launched fighter; carries the loadout and whether it is crewed by an NPC.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getLoadout() {
        return loadout;
    }

    public int getId() {
        return id;
    }

    public boolean isPlayerControlled() {
        return playerControlled;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "LaunchFighter: ", "")
                .add("loadout='" + loadout + "'")
                .add("id=" + id)
                .add("playerControlled=" + playerControlled)
                .toString();
    }
}
