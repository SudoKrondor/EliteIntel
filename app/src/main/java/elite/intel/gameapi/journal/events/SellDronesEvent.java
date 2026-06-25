package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Selling limpet drones back. {@code TotalSale} is an inflow.
 */
public class SellDronesEvent extends BaseEvent {

    @SerializedName("Type")
    private String type;
    @SerializedName("Count")
    private int count;
    @SerializedName("SellPrice")
    private long sellPrice;
    @SerializedName("TotalSale")
    private long totalSale;
    @SerializedName("MarketID")
    private long marketID;

    public SellDronesEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SellDrones");
        SellDronesEvent e = GsonFactory.getGson().fromJson(json, SellDronesEvent.class);
        this.type = e.type;
        this.count = e.count;
        this.sellPrice = e.sellPrice;
        this.totalSale = e.totalSale;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "SellDrones";
    }

    @Override
    public String llmDescription() {
        return "Sold limpet drones; carries the count and value.";
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

    public long getSellPrice() {
        return sellPrice;
    }

    public long getTotalSale() {
        return totalSale;
    }

    public long getMarketID() {
        return marketID;
    }
}
