package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class CarrierTradeOrderEvent extends BaseEvent {
    @SerializedName("CarrierID")
    private long carrierID;

    @SerializedName("CarrierType")
    private String carrierType;

    @SerializedName("BlackMarket")
    private boolean blackMarket;

    @SerializedName("Commodity")
    private String commodity;

    @SerializedName("PurchaseOrder")
    private int purchaseOrder;

    @SerializedName("Price")
    private long price;

    public CarrierTradeOrderEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CarrierTradeOrder");

        // Deserialize ourselves using the same gson instance
        CarrierTradeOrderEvent event = GsonFactory.getGson().fromJson(json, CarrierTradeOrderEvent.class);

        this.carrierID     = event.carrierID;
        this.carrierType   = event.carrierType;
        this.blackMarket   = event.blackMarket;
        this.commodity     = event.commodity;
        this.purchaseOrder = event.purchaseOrder;
        this.price         = event.price;
    }

    @Override
    public String getEventType() {
        return "CarrierTradeOrder";
    }

    @Override
    public String llmDescription() {
        return "Set or cancelled a buy/sell order on the fleet carrier's market; carries the commodity, amount, and price.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    // Getters
    public long getCarrierID() {
        return carrierID;
    }

    public String getCarrierType() {
        return carrierType;
    }

    public boolean isBlackMarket() {
        return blackMarket;
    }

    public String getCommodity() {
        return commodity;
    }

    public int getPurchaseOrder() {
        return purchaseOrder;
    }

    public long getPrice() {
        return price;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "CarrierTradeOrder: ", "")
                .add("carrierID=" + carrierID)
                .add("carrierType='" + carrierType + "'")
                .add("blackMarket=" + blackMarket)
                .add("commodity='" + commodity + "'")
                .add("purchaseOrder=" + purchaseOrder + "t")
                .add("price=" + price + " Cr")
                .toString();
    }
}