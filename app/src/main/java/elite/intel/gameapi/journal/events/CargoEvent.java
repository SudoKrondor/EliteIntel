package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class CargoEvent extends BaseEvent {
    @SerializedName("Vessel")
    private String vessel;

    @SerializedName("Count")
    private int count;

    public CargoEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Cargo");
        CargoEvent event = GsonFactory.getGson().fromJson(json, CargoEvent.class);
        this.vessel = event.vessel;
        this.count = event.count;
    }

    @Override
    public String getEventType() {
        return "Cargo";
    }

    /** Frequent cargo snapshot. Ignore. */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "A snapshot of the current cargo hold; carries the list of commodities and their counts.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getVessel() {
        return vessel;
    }

    public int getCount() {
        return count;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("Cargo detected: ")
                .add("vessel='" + vessel + "'")
                .add("count=" + count)
                .toString();
    }
}