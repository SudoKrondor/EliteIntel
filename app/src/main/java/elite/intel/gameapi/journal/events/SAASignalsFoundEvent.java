package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class SAASignalsFoundEvent extends BaseEvent {
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

    public static class Genus {
        @SerializedName("Genus")
        private String genus;

        @SerializedName("Genus_Localised")
        private String genusLocalised;

        public String getGenus() {
            return genus;
        }

        public String getGenusLocalised() {
            return genusLocalised;
        }
    }

    @SerializedName("BodyName")
    private String bodyName;

    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("BodyID")
    private long bodyID;

    @SerializedName("Signals")
    private List<Signal> signals;

    @SerializedName("Genuses")
    private List<Genus> genuses;

    public SAASignalsFoundEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SAASignalsFound");
        SAASignalsFoundEvent event = GsonFactory.getGson().fromJson(json, SAASignalsFoundEvent.class);
        this.bodyName = event.bodyName;
        this.systemAddress = event.systemAddress;
        this.bodyID = event.bodyID;
        this.signals = event.signals;
        this.genuses = event.genuses;
    }

    @Override
    public String getEventType() {
        return "SAASignalsFound";
    }

    @Override
    public String llmDescription() {
        return "A detailed surface scan found signal sources on a body (geological, biological, human, and so on); carries the body and the signal types with counts.";
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

    public long getBodyID() {
        return bodyID;
    }

    public List<Signal> getSignals() {
        return signals;
    }

    public List<Genus> getGenuses() {
        return genuses;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}