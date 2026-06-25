package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class ScannedEvent extends BaseEvent {
    @SerializedName("ScanType")
    private String scanType;

    public ScannedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Scanned");
        ScannedEvent event = GsonFactory.getGson().fromJson(json, ScannedEvent.class);
        this.scanType = event.scanType;
    }

    @Override
    public String getEventType() {
        return "Scanned";
    }

    @Override
    public String llmDescription() {
        return "Another vessel or authority is scanning you; carries the scan type (cargo, data link, crime, and so on).";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getScanType() {
        return scanType;
    }
}