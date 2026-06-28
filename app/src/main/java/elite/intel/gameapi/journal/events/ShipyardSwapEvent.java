package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ShipyardSwapEvent extends BaseEvent {

    @SerializedName("ShipType")
    private String shipType;

    @SerializedName("ShipType_Localised")
    private String shipTypeLocalised;

    @SerializedName("ShipID")
    private int shipID;

    @SerializedName("StoreOldShip")
    private String storeOldShip;

    @SerializedName("StoreShipID")
    private int storeShipID;

    @SerializedName("MarketID")
    private long marketID;

    public ShipyardSwapEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "ShipyardSwap");
        ShipyardSwapEvent e = GsonFactory.getGson().fromJson(json, ShipyardSwapEvent.class);
        this.shipType = e.shipType;
        this.shipTypeLocalised = e.shipTypeLocalised;
        this.shipID = e.shipID;
        this.storeOldShip = e.storeOldShip;
        this.storeShipID = e.storeShipID;
        this.marketID = e.marketID;
    }

    @Override
    public String getEventType() {
        return "ShipyardSwap";
    }

    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Player boarded a different ship stored at a shipyard; carries the new ship type and optional localised display name.";
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

    public String getShipTypeLocalised() {
        return shipTypeLocalised;
    }

    public int getShipID() {
        return shipID;
    }

    public String getStoreOldShip() {
        return storeOldShip;
    }

    public int getStoreShipID() {
        return storeShipID;
    }

    public long getMarketID() {
        return marketID;
    }
}
