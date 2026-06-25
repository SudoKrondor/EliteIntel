package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.gameapi.MissionType;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

import static elite.intel.util.StringUtls.removeNameEnding;

public class MissionsEvent extends BaseEvent {
    public static class Mission {
        @SerializedName("MissionID")
        private long missionID;

        @SerializedName("Name")
        private String name;

        @SerializedName("PassengerMission")
        private boolean passengerMission;

        @SerializedName("Expires")
        private long expires;

        public long getMissionID() {
            return missionID;
        }

        public String getName() {
            return name;
        }

        public boolean isPassengerMission() {
            return passengerMission;
        }

        public long getExpires() {
            return expires;
        }

        // todo: Need assistance in creating a propper json or usable MissionDto convertion
        public JsonObject toJsonObject() {
            return GsonFactory.toJsonObject(this);
        }

        public MissionType getMissionType() {
            String missionType = removeNameEnding(getName());
            for (MissionType type : MissionType.values()) {
                if (type.getMissionType().equalsIgnoreCase(missionType)) {
                    return type;
                }
            }
            return MissionType.getUnknown();
//        throw new IllegalArgumentException("Unknown mission type: " + name);
        }
    }

    @SerializedName("Active")
    private List<Mission> active;

    @SerializedName("Failed")
    private List<Mission> failed;

    @SerializedName("Complete")
    private List<Mission> complete;

    public MissionsEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofHours(24), "Missions");
        MissionsEvent event = GsonFactory.getGson().fromJson(json, MissionsEvent.class);
        this.active = event.active;
        this.failed = event.failed;
        this.complete = event.complete;
    }

    @Override
    public String getEventType() {
        return "Missions";
    }

    @Override
    public String llmDescription() {
        return "A snapshot of all active, completed, and failed missions; written at game load. Background list.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<Mission> getActive() {
        return active;
    }

    public List<Mission> getFailed() {
        return failed;
    }

    public List<Mission> getComplete() {
        return complete;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }
}