package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class ShipyardBuyEvent extends BaseEvent {

    @SerializedName("ShipType")
    private String shipType;

    @SerializedName("ShipPrice")
    private long shipPrice;

    /**
     * Only present when an existing ship is stored rather than sold.
     */
    @SerializedName("StoreOldShip")
    private String storeOldShip;

    @SerializedName("StoreShipID")
    private int storeShipID;

    /**
     * Trade-in value, only present when the old ship is sold rather than stored. 0 otherwise.
     */
    @SerializedName("SellPrice")
    private long sellPrice;

    @SerializedName("SellOldShip")
    private String sellOldShip;

    @SerializedName("MarketID")
    private long marketID;

    public ShipyardBuyEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipyardBuy");
        ShipyardBuyEvent event = GsonFactory.getGson().fromJson(json, ShipyardBuyEvent.class);
        this.shipType = event.shipType;
        this.shipPrice = event.shipPrice;
        this.storeOldShip = event.storeOldShip;
        this.storeShipID = event.storeShipID;
        this.sellPrice = event.sellPrice;
        this.sellOldShip = event.sellOldShip;
        this.marketID = event.marketID;
    }

    @Override
    public String getEventType() {
        return "ShipyardBuy";
    }

    @Override
    public String llmDescription() {
        return "Bought a new ship at a shipyard; carries the ship type and the price paid.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getShipType() {
        return shipType;
    }

    public long getShipPrice() {
        return shipPrice;
    }

    /**
     * Null if no ship was stored (e.g. player had no existing ship).
     */
    public String getStoreOldShip() {
        return storeOldShip;
    }

    public int getStoreShipID() {
        return storeShipID;
    }

    /**
     * Trade-in value for the sold old ship; 0 when the old ship was stored or none existed.
     */
    public long getSellPrice() {
        return sellPrice;
    }

    public String getSellOldShip() {
        return sellOldShip;
    }

    public long getMarketID() {
        return marketID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp(), useLocalTime);
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "ShipyardBuy: ", "")
                .add("shipType='" + shipType + "'")
                .add("price=" + shipPrice);
        if (storeOldShip != null) sj.add("stored='" + storeOldShip + "'");
        return sj.toString();
    }
}
