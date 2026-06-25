package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class MissionRedirectedEvent extends BaseEvent {

    @SerializedName("Name")
    private String name;

    @SerializedName("LocalisedName")
    private String localisedName;

    @SerializedName("NewDestinationStation")
    private String newDestinationStation;

    @SerializedName("NewDestinationSystem")
    private String newDestinationSystem;

    @SerializedName("OldDestinationStation")
    private String oldDestinationStation;

    @SerializedName("OldDestinationSystem")
    private String oldDestinationSystem;

    @SerializedName("MissionID")
    private long missionID;

    public MissionRedirectedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "MissionRedirected");
        MissionRedirectedEvent event = GsonFactory.getGson().fromJson(json, MissionRedirectedEvent.class);
        this.missionID = event.missionID;
        this.name = event.name;
        this.localisedName = event.localisedName;
        this.newDestinationStation = event.newDestinationStation;
        this.newDestinationSystem = event.newDestinationSystem;
    }

    @Override
    public String getEventType() {
        return "MissionRedirected";
    }

    @Override
    public String llmDescription() {
        return "A mission objective was redirected to a new destination; carries the mission name and the new system and station.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public Long getMissionID() { return missionID; }

    public String getName() {
        return name;
    }

    public String getLocalisedName() {
        return localisedName;
    }

    public String getNewDestinationStation() { return newDestinationStation; }

    public String getNewDestinationSystem() { return newDestinationSystem; }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}
