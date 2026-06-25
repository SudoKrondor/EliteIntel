package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class DockingGrantedEvent extends BaseEvent {

    @SerializedName("LandingPad")
    private int landingPad;

    @SerializedName("MarketID")
    private long marketID;

    @SerializedName("StationName")
    private String stationName;

    @SerializedName("StationType")
    private String stationType;

    public DockingGrantedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "DockingGranted");
        DockingGrantedEvent event = GsonFactory.getGson().fromJson(json, DockingGrantedEvent.class);
        this.landingPad = event.landingPad;
        this.marketID = event.marketID;
        this.stationName = event.stationName;
        this.stationType = event.stationType;
    }

    @Override
    public String getEventType() {
        return "DockingGranted";
    }

    /** Station comms already voice the grant; record only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "A station granted your docking request; carries the station name and the assigned landing pad.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public int getLandingPad() {
        return landingPad;
    }

    public long getMarketID() {
        return marketID;
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationType() {
        return stationType;
    }
}
