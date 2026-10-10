package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Emitted when the full-spectrum scanner has resolved every body the discovery scan counted. The
 * explorer card reads it as the end of the FSS, which is exact where counting stored body rows is not.
 */
public class FSSAllBodiesFoundEvent extends BaseEvent {
    @SerializedName("SystemName")
    private String systemName;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("Count")
    private int count;

    public FSSAllBodiesFoundEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "FSSAllBodiesFound");
        FSSAllBodiesFoundEvent event = GsonFactory.getGson().fromJson(json, FSSAllBodiesFoundEvent.class);
        this.systemName = event.systemName;
        this.systemAddress = event.systemAddress;
        this.count = event.count;
    }

    @Override
    public String getEventType() {
        return "FSSAllBodiesFound";
    }

    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "The full-spectrum scanner has found every body in the current system.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getSystemName() {
        return systemName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getCount() {
        return count;
    }
}
