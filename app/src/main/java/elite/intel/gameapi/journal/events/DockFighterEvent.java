package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class DockFighterEvent extends BaseEvent {

    @SerializedName("ID")
    private int id;

    public DockFighterEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "DockFighter");
        DockFighterEvent event = GsonFactory.getGson().fromJson(json, DockFighterEvent.class);
        this.id = event.id;
    }

    @Override
    public String getEventType() {
        return "DockFighter";
    }

    /** Routine fighter recall; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Docked a deployed ship-launched fighter back into the mother ship.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public int getId() {
        return id;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "DockFighter: ", "")
                .add("id=" + id)
                .toString();
    }
}
