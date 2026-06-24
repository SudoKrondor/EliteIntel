package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Repair-all at a station. {@code Cost} is an outflow.
 */
public class RepairAllEvent extends BaseEvent {

    @SerializedName("Cost")
    private long cost;

    public RepairAllEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "RepairAll");
        RepairAllEvent e = GsonFactory.getGson().fromJson(json, RepairAllEvent.class);
        this.cost = e.cost;
    }

    @Override
    public String getEventType() {
        return "RepairAll";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCost() {
        return cost;
    }
}
