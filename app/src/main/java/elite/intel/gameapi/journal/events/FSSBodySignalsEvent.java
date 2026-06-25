package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class FSSBodySignalsEvent extends BaseEvent {
    public static class Signal {
        @SerializedName("Type")
        private String type;

        @SerializedName("Type_Localised")
        private String typeLocalised;

        @SerializedName("Count")
        private int count;

        public String getType() {
            return type;
        }

        public String getTypeLocalised() {
            return typeLocalised;
        }

        public int getCount() {
            return count;
        }
    }

    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("BodyID")
    private Long bodyID;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("Signals")
    private List<Signal> signals;

    public FSSBodySignalsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "FSSBodySignals");
        FSSBodySignalsEvent event = GsonFactory.getGson().fromJson(json, FSSBodySignalsEvent.class);
        this.bodyName = event.bodyName;
        this.bodyID = event.bodyID;
        this.systemAddress = event.systemAddress;
        this.signals = event.signals;
    }

    @Override
    public String getEventType() {
        return "FSSBodySignals";
    }

    @Override
    public String llmDescription() {
        return "The full-spectrum scanner reported signal sources on a body (for example biological or geological counts); carries the body and the signal list.";
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

    public Long getBodyID() {
        return bodyID;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public List<Signal> getSignals() {
        return signals;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}