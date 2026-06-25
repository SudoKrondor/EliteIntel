package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Selling a stored ship. {@code ShipPrice} is an inflow.
 */
public class ShipyardSellEvent extends BaseEvent {

    @SerializedName("ShipType")
    private String shipType;
    @SerializedName("SellShipID")
    private int sellShipID;
    @SerializedName("ShipPrice")
    private long shipPrice;
    @SerializedName("MarketID")
    private long marketID;

    public ShipyardSellEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipyardSell");
        ShipyardSellEvent e = GsonFactory.getGson().fromJson(json, ShipyardSellEvent.class);
        this.shipType = e.shipType;
        this.sellShipID = e.sellShipID;
        this.shipPrice = e.shipPrice;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "ShipyardSell";
    }

    @Override
    public String llmDescription() {
        return "Sold a stored ship; carries the ship type and the value.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getShipType() {
        return shipType;
    }

    public int getSellShipID() {
        return sellShipID;
    }

    public long getShipPrice() {
        return shipPrice;
    }

    public long getMarketID() {
        return marketID;
    }
}
