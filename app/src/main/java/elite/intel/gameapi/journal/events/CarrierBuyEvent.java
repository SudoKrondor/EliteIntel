package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Purchasing a fleet carrier. {@code Price} is a (large) outflow.
 */
public class CarrierBuyEvent extends BaseEvent {

    @SerializedName("CarrierID")
    private long carrierID;
    @SerializedName("BoughtAtMarket")
    private long boughtAtMarket;
    @SerializedName("Location")
    private String location;
    @SerializedName("Price")
    private long price;
    @SerializedName("Callsign")
    private String callsign;

    public CarrierBuyEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CarrierBuy");
        CarrierBuyEvent e = GsonFactory.getGson().fromJson(json, CarrierBuyEvent.class);
        this.carrierID = e.carrierID;
        this.boughtAtMarket = e.boughtAtMarket;
        this.location = e.location;
        this.price = e.price;
        this.callsign = e.callsign;
    }

    @Override
    public String getEventType() {
        return "CarrierBuy";
    }

    /** Bought a fleet carrier; major purchase. */
    @Override
    public Importance importance() {
        return Importance.HIGH;
    }

    @Override
    public String llmDescription() {
        return "Purchased a fleet carrier; carries the deployment location and the price.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getCarrierID() {
        return carrierID;
    }

    public long getBoughtAtMarket() {
        return boughtAtMarket;
    }

    public String getLocation() {
        return location;
    }

    public long getPrice() {
        return price;
    }

    public String getCallsign() {
        return callsign;
    }
}
