package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class CarrierLocationEvent extends BaseEvent {
    @SerializedName("CarrierType")
    private String carrierType;

    @SerializedName("CarrierID")
    private long carrierID;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private int bodyID;

    public CarrierLocationEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(7), "CarrierLocation");
        CarrierLocationEvent event = GsonFactory.getGson().fromJson(json, CarrierLocationEvent.class);
        this.carrierType = event.carrierType;
        this.carrierID = event.carrierID;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
    }

    @Override
    public String getEventType() {
        return "CarrierLocation";
    }

    /** Carrier location snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Reports the fleet carrier's current star system, for example after it jumped.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getCarrierType() {
        return carrierType;
    }

    public long getCarrierID() {
        return carrierID;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyID() {
        return bodyID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}