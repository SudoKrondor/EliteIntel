package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Emitted when the full-spectrum scanner "honk" completes its discovery sweep of the current
 * system. Carries the body/non-body tallies and the honk progress. {@code HonkCommand} listens
 * for this event so it can release the fire trigger exactly when the sweep registers instead of
 * guessing a fixed hold duration.
 */
public class FSSDiscoveryScanEvent extends BaseEvent {
    @SerializedName("Progress")
    private double progress;

    @SerializedName("BodyCount")
    private int bodyCount;

    @SerializedName("NonBodyCount")
    private int nonBodyCount;

    @SerializedName("SystemName")
    private String systemName;

    @SerializedName("SystemAddress")
    private long systemAddress;

    public FSSDiscoveryScanEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "FSSDiscoveryScan");
        FSSDiscoveryScanEvent event = GsonFactory.getGson().fromJson(json, FSSDiscoveryScanEvent.class);
        this.progress = event.progress;
        this.bodyCount = event.bodyCount;
        this.nonBodyCount = event.nonBodyCount;
        this.systemName = event.systemName;
        this.systemAddress = event.systemAddress;
    }

    @Override
    public String getEventType() {
        return "FSSDiscoveryScan";
    }

    /**
     * Honk completion; mainly drives the discovery-scan command timing.
     */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "The discovery scanner (honk) finished sweeping the current system; reports how many bodies and non-body signals it detected.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public double getProgress() {
        return progress;
    }

    public int getBodyCount() {
        return bodyCount;
    }

    public int getNonBodyCount() {
        return nonBodyCount;
    }

    public String getSystemName() {
        return systemName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}