package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Buying/restocking an SRV or fighter. {@code Cost} is an outflow.
 */
public class RestockVehicleEvent extends BaseEvent {

    @SerializedName("Type")
    private String type;
    @SerializedName("Loadout")
    private String loadout;
    @SerializedName("Cost")
    private long cost;
    @SerializedName("Count")
    private int count;

    public RestockVehicleEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "RestockVehicle");
        RestockVehicleEvent e = GsonFactory.getGson().fromJson(json, RestockVehicleEvent.class);
        this.type = e.type;
        this.loadout = e.loadout;
        this.cost = e.cost;
        this.count = e.count;
    }

    @Override
    public String getEventType() {
        return "RestockVehicle";
    }

    /** Routine restock spend; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Restocked SRVs or fighters; carries the type, count, and cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getType() {
        return type;
    }

    public String getLoadout() {
        return loadout;
    }

    public long getCost() {
        return cost;
    }

    public int getCount() {
        return count;
    }
}
