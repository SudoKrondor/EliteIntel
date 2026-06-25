package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class CarrierJumpRequestEvent extends BaseEvent {
    @SerializedName("CarrierType")
    private String carrierType;

    @SerializedName("CarrierID")
    private long carrierId;

    @SerializedName("SystemName")
    private String systemName;

    @SerializedName("Body")
    private String body;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private int bodyId;

    @SerializedName("DepartureTime")
    private String departureTime;

    public CarrierJumpRequestEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CarrierJumpRequest");
        CarrierJumpRequestEvent event = GsonFactory.getGson().fromJson(json, CarrierJumpRequestEvent.class);
        this.carrierType = event.carrierType;
        this.carrierId = event.carrierId;
        this.systemName = event.systemName;
        this.body = event.body;
        this.systemAddress = event.systemAddress;
        this.bodyId = event.bodyId;
        this.departureTime = event.departureTime;
    }

    @Override
    public String getEventType() {
        return "CarrierJumpRequest";
    }

    @Override
    public String llmDescription() {
        return "A fleet carrier jump was scheduled; carries the destination system, body, and departure time.";
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

    public long getCarrierId() {
        return carrierId;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getBody() {
        return body;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyId() {
        return bodyId;
    }

    public String getDepartureTime() {
        return departureTime;
    }
}