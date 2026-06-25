package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class FSSSignalDiscoveredEvent extends BaseEvent {
    @SerializedName("SystemAddress")
    private long systemAddress;

    @SerializedName("SignalName")
    private String signalName;

    @SerializedName("SignalName_Localised")
    private String signalNameLocalised;

    @SerializedName("SignalType")
    private String signalType;

    @SerializedName("USSType")
    private String ussType;

    @SerializedName("USSType_Localised")
    private String ussTypeLocalised;

    @SerializedName("SpawningState")
    private String spawningState;

    @SerializedName("SpawningState_Localised")
    private String spawningStateLocalised;

    @SerializedName("SpawningFaction")
    private String spawningFaction;

    @SerializedName("SpawningFaction_Localised")
    private String spawningFactionLocalised;

    @SerializedName("ThreatLevel")
    private int threatLevel;

    @SerializedName("TimeRemaining")
    private double timeRemaining;

    public FSSSignalDiscoveredEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "FSSSignalDiscovered");
        FSSSignalDiscoveredEvent event = GsonFactory.getGson().fromJson(json, FSSSignalDiscoveredEvent.class);
        this.systemAddress = event.systemAddress;
        this.signalName = event.signalName;
        this.signalNameLocalised = event.signalNameLocalised;
        this.signalType = event.signalType;
        this.ussType = event.ussType;
        this.ussTypeLocalised = event.ussTypeLocalised;
        this.spawningState = event.spawningState;
        this.spawningStateLocalised = event.spawningStateLocalised;
        this.spawningFaction = event.spawningFaction;
        this.spawningFactionLocalised = event.spawningFactionLocalised;
        this.threatLevel = event.threatLevel;
        this.timeRemaining = event.timeRemaining;
    }

    @Override
    public String getEventType() {
        return "FSSSignalDiscovered";
    }

    @Override
    public String llmDescription() {
        return "The full-spectrum scanner picked up one signal source in the system (station, fleet carrier, unidentified signal, resource site, and so on). Very high frequency: one per signal.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getSignalName() {
        return signalName;
    }

    public String getSignalNameLocalised() {
        return signalNameLocalised;
    }

    public String getSignalType() {
        return signalType;
    }

    public String getUssType() {
        return ussType;
    }

    public String getUssTypeLocalised() {
        return ussTypeLocalised;
    }

    public String getSpawningState() {
        return spawningState;
    }

    public String getSpawningStateLocalised() {
        return spawningStateLocalised;
    }

    public String getSpawningFaction() {
        return spawningFaction;
    }

    public String getSpawningFactionLocalised() {
        return spawningFactionLocalised;
    }

    public int getThreatLevel() {
        return threatLevel;
    }

    public double getTimeRemaining() {
        return timeRemaining;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}