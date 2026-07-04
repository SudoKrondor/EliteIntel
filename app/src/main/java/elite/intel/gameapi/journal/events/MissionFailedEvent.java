package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class MissionFailedEvent extends BaseEvent {
    @SerializedName("Name")
    private String name;

    @SerializedName("LocalisedName")
    private String localisedName;

    @SerializedName("MissionID")
    private long missionID;

    public MissionFailedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "MissionFailed");
        MissionFailedEvent event = GsonFactory.getGson().fromJson(json, MissionFailedEvent.class);
        this.name = event.name;
        this.localisedName = event.localisedName;
        this.missionID = event.missionID;
    }

    @Override
    public String getEventType() {
        return "MissionFailed";
    }

    /** Mission setback worth a remark. */
    @Override
    public Importance importance() {
        return Importance.HIGH;
    }

    @Override
    public String llmDescription() {
        return "A mission failed; carries the mission name and the giving faction.";
    }

    @Override
    public String memorySummary() {
        String missionName = localisedName != null && !localisedName.isBlank() ? localisedName : name;
        return missionName == null || missionName.isBlank() ? "" : "mission failed: " + missionName;
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getName() {
        return name;
    }

    public String getLocalisedName() {
        return localisedName;
    }

    public long getMissionID() {
        return missionID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}