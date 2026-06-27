package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Transferring a stored ship to the current station. {@code TransferPrice} is an outflow.
 */
public class ShipyardTransferEvent extends BaseEvent {

    @SerializedName("ShipType")
    private String shipType;
    @SerializedName("ShipID")
    private int shipID;
    @SerializedName("System")
    private String system;
    @SerializedName("Distance")
    private double distance;
    @SerializedName("TransferPrice")
    private long transferPrice;
    @SerializedName("MarketID")
    private long marketID;

    public ShipyardTransferEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipyardTransfer");
        ShipyardTransferEvent e = GsonFactory.getGson().fromJson(json, ShipyardTransferEvent.class);
        this.shipType = e.shipType;
        this.shipID = e.shipID;
        this.system = e.system;
        this.distance = e.distance;
        this.transferPrice = e.transferPrice;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "ShipyardTransfer";
    }

    /** Ship transfer ordered; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Requested transfer of a stored ship to the current station; carries the ship, distance, transfer time, and cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getShipType() {
        return shipType;
    }

    public int getShipID() {
        return shipID;
    }

    public String getSystem() {
        return system;
    }

    public double getDistance() {
        return distance;
    }

    public long getTransferPrice() {
        return transferPrice;
    }

    public long getMarketID() {
        return marketID;
    }
}
