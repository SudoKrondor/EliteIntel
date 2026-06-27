package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Partial refuel (e.g. fuel scoop top-up purchase). {@code Cost} is an outflow.
 */
public class RefuelPartialEvent extends BaseEvent {

    @SerializedName("Cost")
    private long cost;
    @SerializedName("Amount")
    private double amount;

    public RefuelPartialEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "RefuelPartial");
        RefuelPartialEvent e = GsonFactory.getGson().fromJson(json, RefuelPartialEvent.class);
        this.cost = e.cost;
        this.amount = e.amount;
    }

    @Override
    public String getEventType() {
        return "RefuelPartial";
    }

    /** Routine refuel spend; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Partially refueled the ship (for example via an auto-refuel service); carries the cost and amount.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCost() {
        return cost;
    }

    public double getAmount() {
        return amount;
    }
}
