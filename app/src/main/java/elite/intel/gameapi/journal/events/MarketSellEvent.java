package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class MarketSellEvent extends BaseEvent {

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("Type")
    private String type;

    @SerializedName("Count")
    private int count;

    @SerializedName("SellPrice")
    private long sellPrice;

    @SerializedName("TotalSale")
    private long totalSale;

    @SerializedName("AvgPricePaid")
    private long avgPricePaid;

    public MarketSellEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "MarketSell");
        MarketSellEvent event = GsonFactory.getGson().fromJson(json, MarketSellEvent.class);
        this.marketID = event.marketID;
        this.type = event.type;
        this.count = event.count;
        this.sellPrice = event.sellPrice;
        this.totalSale = event.totalSale;
        this.avgPricePaid = event.avgPricePaid;
    }

    @Override
    public String getEventType() {
        return "MarketSell";
    }

    @Override
    public String llmDescription() {
        return "Sold cargo at a station market; carries the commodity, quantity sold, and total sale value.";
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
    public long getSellPrice() { return sellPrice; }
    public long getTotalSale() { return totalSale; }
    public long getAvgPricePaid() { return avgPricePaid; }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "MarketSell: ", "")
                .add("type='" + type + "'")
                .add("count=" + count)
                .add("price=" + sellPrice)
                .add("total=" + totalSale)
                .add("avgPaid=" + avgPricePaid)
                .toString();
    }
}