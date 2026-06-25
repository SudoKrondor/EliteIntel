package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Repair of a single item/module. {@code Cost} is an outflow.
 */
public class RepairEvent extends BaseEvent {

    @SerializedName("Item")
    private String item;
    @SerializedName("Cost")
    private long cost;

    public RepairEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Repair");
        RepairEvent e = GsonFactory.getGson().fromJson(json, RepairEvent.class);
        this.item = e.item;
        this.cost = e.cost;
    }

    @Override
    public String getEventType() {
        return "Repair";
    }

    @Override
    public String llmDescription() {
        return "Paid to repair a specific item or module; carries the item and the cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getItem() {
        return item;
    }

    public long getCost() {
        return cost;
    }
}
