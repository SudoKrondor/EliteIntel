package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class SupercruiseEntryEvent extends BaseEvent {
    @SerializedName("Taxi")
    private boolean taxi;

    @SerializedName("Multicrew")
    private boolean multicrew;

    @SerializedName("StarSystem")
    private String starSystem;

    @SerializedName("SystemAddress")
    private long systemAddress;

    public SupercruiseEntryEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "SupercruiseEntry");
        SupercruiseEntryEvent event = GsonFactory.getGson().fromJson(json, SupercruiseEntryEvent.class);
        this.taxi = event.taxi;
        this.multicrew = event.multicrew;
        this.starSystem = event.starSystem;
        this.systemAddress = event.systemAddress;
    }

    @Override
    public String getEventType() {
        return "SupercruiseEntry";
    }

    @Override
    public String llmDescription() {
        return "Entered supercruise from normal space; carries the current star system.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public boolean isTaxi() {
        return taxi;
    }

    public boolean isMulticrew() {
        return multicrew;
    }

    public String getStarSystem() {
        return starSystem;
    }

    public long getSystemAddress() {
        return systemAddress;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}