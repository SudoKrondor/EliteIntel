package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * A mission's cargo depot moved: goods were collected for a delivery mission, or handed in against
 * one. A mining mission is the case that matters - its tonnes go in one load at a time, and this is
 * the only record of how many of them a mission has already taken.
 * <p>
 * {@code ItemsDelivered} is the running total for the mission, not this load, so the latest event
 * alone says where a mission stands.
 */
public class CargoDepotEvent extends BaseEvent {

    @SerializedName("MissionID")
    private long missionID;

    @SerializedName("UpdateType")
    private String updateType;

    @SerializedName("CargoType")
    private String cargoType;

    @SerializedName("Count")
    private int count;

    @SerializedName("ItemsCollected")
    private int itemsCollected;

    @SerializedName("ItemsDelivered")
    private int itemsDelivered;

    @SerializedName("TotalItemsToDeliver")
    private int totalItemsToDeliver;

    public CargoDepotEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CargoDepot");
        CargoDepotEvent event = GsonFactory.getGson().fromJson(json, CargoDepotEvent.class);
        this.missionID = event.missionID;
        this.updateType = event.updateType;
        this.cargoType = event.cargoType;
        this.count = event.count;
        this.itemsCollected = event.itemsCollected;
        this.itemsDelivered = event.itemsDelivered;
        this.totalItemsToDeliver = event.totalItemsToDeliver;
    }

    @Override
    public String getEventType() {
        return "CargoDepot";
    }

    /** One load of a mission's cargo; the game already shows the hand-in. */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Collected or delivered cargo at a mission depot; carries the mission, the commodity and how many units the mission has taken so far.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMissionID() {
        return missionID;
    }

    public String getUpdateType() {
        return updateType;
    }

    public String getCargoType() {
        return cargoType;
    }

    public int getCount() {
        return count;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    /** Units the mission has taken so far, across every load. */
    public int getItemsDelivered() {
        return itemsDelivered;
    }

    public int getTotalItemsToDeliver() {
        return totalItemsToDeliver;
    }
}
