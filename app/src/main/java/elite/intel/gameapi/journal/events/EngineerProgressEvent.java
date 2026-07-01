package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class EngineerProgressEvent extends BaseEvent {
    @SerializedName("Engineers")
    private List<Engineer> engineers;

    public EngineerProgressEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofDays(30), "EngineerProgress");
        EngineerProgressEvent event = GsonFactory.getGson().fromJson(json, EngineerProgressEvent.class);
        this.engineers = event.engineers;
    }

    @Override
    public String getEventType() {
        return "EngineerProgress";
    }

    /** Engineer progress snapshot; memory context. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Engineer unlock or progress update; carries the engineer and your rank or progress. Snapshot.";
    }

    @Override
    public String memorySummary() {
        return engineers == null || engineers.isEmpty() ? "" : "engineer progress: " + engineers.size() + " engineers on record";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<Engineer> getEngineers() {
        return engineers;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    public static class Engineer {
        @SerializedName("Engineer")
        private String name;

        @SerializedName("EngineerID")
        private long engineerID;

        @SerializedName("Progress")
        private String progress;

        @SerializedName("RankProgress")
        private int rankProgress;

        @SerializedName("Rank")
        private int rank;

        public String getName() {
            return name;
        }

        public long getEngineerID() {
            return engineerID;
        }

        public String getProgress() {
            return progress;
        }

        public int getRankProgress() {
            return rankProgress;
        }

        public int getRank() {
            return rank;
        }

        public boolean isFullyUnlocked() {
            return "Unlocked".equals(progress) && rank == 5 && rankProgress == 0;
        }
    }
}