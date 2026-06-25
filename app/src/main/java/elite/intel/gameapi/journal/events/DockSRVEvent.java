package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class DockSRVEvent extends BaseEvent {
    @SerializedName("SRVType")
    private String srvType;

    @SerializedName("SRVType_Localised")
    private String srvTypeLocalised;

    @SerializedName("ID")
    private int id;

    public DockSRVEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "DockSRV");
        DockSRVEvent event = GsonFactory.getGson().fromJson(json, DockSRVEvent.class);
        this.srvType = event.srvType;
        this.srvTypeLocalised = event.srvTypeLocalised;
        this.id = event.id;
    }

    @Override
    public String getEventType() {
        return "DockSRV";
    }

    @Override
    public String llmDescription() {
        return "Recalled the SRV back aboard the ship.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getSrvType() {
        return srvType;
    }

    public String getSrvTypeLocalised() {
        return srvTypeLocalised;
    }

    public int getId() {
        return id;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}