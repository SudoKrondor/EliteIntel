package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Buying limpet drones. {@code TotalCost} is an outflow.
 */
public class BuyDronesEvent extends BaseEvent {

    @SerializedName("Type")
    private String type;
    @SerializedName("Count")
    private int count;
    // WHY: docs are inconsistent on the per-unit key for BuyDrones (some list "SellPrice"); the
    // finance delta uses TotalCost only, so this field is informational and may stay 0.
    @SerializedName("BuyPrice")
    private long buyPrice;
    @SerializedName("TotalCost")
    private long totalCost;
    @SerializedName("MarketID")
    private long marketID;

    public BuyDronesEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "BuyDrones");
        BuyDronesEvent e = GsonFactory.getGson().fromJson(json, BuyDronesEvent.class);
        this.type = e.type;
        this.count = e.count;
        this.buyPrice = e.buyPrice;
        this.totalCost = e.totalCost;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "BuyDrones";
    }

    /** Routine limpet purchase; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Bought limpet drones; carries the count and cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public long getBuyPrice() {
        return buyPrice;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public long getMarketID() {
        return marketID;
    }
}
