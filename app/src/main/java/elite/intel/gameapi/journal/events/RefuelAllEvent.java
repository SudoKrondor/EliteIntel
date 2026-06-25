package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Refuel-all at a station. {@code Cost} is an outflow.
 */
public class RefuelAllEvent extends BaseEvent {

    @SerializedName("Cost")
    private long cost;
    @SerializedName("Amount")
    private double amount;

    public RefuelAllEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "RefuelAll");
        RefuelAllEvent e = GsonFactory.getGson().fromJson(json, RefuelAllEvent.class);
        this.cost = e.cost;
        this.amount = e.amount;
    }

    @Override
    public String getEventType() {
        return "RefuelAll";
    }

    /** Routine refuel spend; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Refueled the ship completely at a station; carries the cost and amount.";
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
