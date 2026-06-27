package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

public class LaunchSRVEvent extends BaseEvent {
    @SerializedName("SRVType")
    private String srvType;

    @SerializedName("SRVType_Localised")
    private String srvTypeLocalised;

    @SerializedName("Loadout")
    private String loadout;

    @SerializedName("ID")
    private int id;

    @SerializedName("PlayerControlled")
    private boolean playerControlled;

    public LaunchSRVEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "LaunchSRV");
        LaunchSRVEvent event = GsonFactory.getGson().fromJson(json, LaunchSRVEvent.class);
        this.srvType = event.srvType;
        this.srvTypeLocalised = event.srvTypeLocalised;
        this.loadout = event.loadout;
        this.id = event.id;
        this.playerControlled = event.playerControlled;
    }

    @Override
    public String getEventType() {
        return "LaunchSRV";
    }

    /** Routine SRV deploy; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Deployed an SRV from the ship onto the planetary surface; carries the SRV loadout.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getSrvType() {
        return srvType;
    }

    public String getSrvTypeLocalised() {
        return srvTypeLocalised;
    }

    public String getLoadout() {
        return loadout;
    }

    public int getId() {
        return id;
    }

    public boolean isPlayerControlled() {
        return playerControlled;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}