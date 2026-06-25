package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class SAAScanCompleteEvent extends BaseEvent {
    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private int bodyID;

    @SerializedName("ProbesUsed")
    private int probesUsed;

    @SerializedName("EfficiencyTarget")
    private int efficiencyTarget;

    public SAAScanCompleteEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SAAScanComplete");
        SAAScanCompleteEvent event = GsonFactory.getGson().fromJson(json, SAAScanCompleteEvent.class);
        this.bodyName = event.bodyName;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
        this.probesUsed = event.probesUsed;
        this.efficiencyTarget = event.efficiencyTarget;
    }

    @Override
    public String getEventType() {
        return "SAAScanComplete";
    }

    @Override
    public String llmDescription() {
        return "Completed mapping a body with the detailed surface scanner; carries the body name and probes used.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getBodyName() {
        return bodyName;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public int getBodyID() {
        return bodyID;
    }

    public int getProbesUsed() {
        return probesUsed;
    }

    public int getEfficiencyTarget() {
        return efficiencyTarget;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}