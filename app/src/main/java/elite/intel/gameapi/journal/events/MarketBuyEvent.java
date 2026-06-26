package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class MarketBuyEvent extends BaseEvent {

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("Type")
    private String type;

    @SerializedName("Count")
    private int count;

    @SerializedName("BuyPrice")
    private long buyPrice;

    @SerializedName("TotalCost")
    private long totalCost;

    public MarketBuyEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MarketBuy");
        MarketBuyEvent event = GsonFactory.getGson().fromJson(json, MarketBuyEvent.class);
        this.marketID = event.marketID;
        this.type = event.type;
        this.count = event.count;
        this.buyPrice = event.buyPrice;
        this.totalCost = event.totalCost;
    }

    @Override
    public String getEventType() {
        return "MarketBuy";
    }

    /** Routine purchase; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Bought cargo at a station market; carries the commodity, quantity, and total price paid.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getMarketID() { return marketID; }
    public String getType() { return type; }
    public int getCount() { return count; }
    public long getBuyPrice() { return buyPrice; }
    public long getTotalCost() { return totalCost; }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "MarketBuy: ", "")
                .add("type='" + type + "'")
                .add("count=" + count)
                .add("price=" + buyPrice)
                .add("total=" + totalCost)
                .toString();
    }
}