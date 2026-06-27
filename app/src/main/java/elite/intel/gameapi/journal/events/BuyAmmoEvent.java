package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Restocking ammunition/limpets ammo at a station. {@code Cost} is an outflow.
 */
public class BuyAmmoEvent extends BaseEvent {

    @SerializedName("Cost")
    private long cost;

    public BuyAmmoEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "BuyAmmo");
        BuyAmmoEvent e = GsonFactory.getGson().fromJson(json, BuyAmmoEvent.class);
        this.cost = e.cost;
    }

    @Override
    public String getEventType() {
        return "BuyAmmo";
    }

    /** Routine restock spend; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Restocked weapon ammunition; carries the cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCost() {
        return cost;
    }
}
