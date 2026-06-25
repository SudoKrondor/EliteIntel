package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public class MissionCompletedEvent extends BaseEvent {
    @SerializedName("Faction")
    private String faction;

    @SerializedName("Name")
    private String name;

    @SerializedName("LocalisedName")
    private String localisedName;

    @SerializedName("MissionID")
    private long missionID;

    @SerializedName("TargetType")
    private String targetType;

    @SerializedName("TargetType_Localised")
    private String targetTypeLocalised;

    @SerializedName("TargetFaction")
    private String targetFaction;

    @SerializedName("KillCount")
    private int killCount;

    @SerializedName("DestinationSystem")
    private String destinationSystem;

    @SerializedName("DestinationStation")
    private String destinationStation;

    @SerializedName("Reward")
    private long reward;

    @SerializedName("FactionEffects")
    private List<FactionEffect> factionEffects;

    public MissionCompletedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "MissionCompleted");
        MissionCompletedEvent event = GsonFactory.getGson().fromJson(json, MissionCompletedEvent.class);
        this.faction = event.faction;
        this.name = event.name;
        this.localisedName = event.localisedName;
        this.missionID = event.missionID;
        this.targetType = event.targetType;
        this.targetTypeLocalised = event.targetTypeLocalised;
        this.targetFaction = event.targetFaction;
        this.killCount = event.killCount;
        this.destinationSystem = event.destinationSystem;
        this.destinationStation = event.destinationStation;
        this.reward = event.reward;
        this.factionEffects = event.factionEffects;
    }

    @Override
    public String getEventType() {
        return "MissionCompleted";
    }

    @Override
    public String llmDescription() {
        return "Completed a mission; carries the mission name, the giving faction, and the reward (credits, materials, or commodities).";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getFaction() {
        return faction;
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

    public String getTargetType() {
        return targetType;
    }

    public String getTargetTypeLocalised() {
        return targetTypeLocalised;
    }

    public String getTargetFaction() {
        return targetFaction;
    }

    public int getKillCount() {
        return killCount;
    }

    public String getDestinationSystem() {
        return destinationSystem;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public long getReward() {
        return reward;
    }

    public List<FactionEffect> getFactionEffects() {
        return factionEffects;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    public static class FactionEffect {
        @SerializedName("Faction")
        private String faction;

        @SerializedName("Effects")
        private List<Effect> effects;

        @SerializedName("Influence")
        private List<Influence> influence;

        @SerializedName("ReputationTrend")
        private String reputationTrend;

        @SerializedName("Reputation")
        private String reputation;

        public String getFaction() {
            return faction;
        }

        public List<Effect> getEffects() {
            return effects;
        }

        public List<Influence> getInfluence() {
            return influence;
        }

        public String getReputationTrend() {
            return reputationTrend;
        }

        public String getReputation() {
            return reputation;
        }
    }

    public static class Effect {
        @SerializedName("Effect")
        private String effect;

        @SerializedName("Effect_Localised")
        private String effectLocalised;

        @SerializedName("Trend")
        private String trend;

        public String getEffect() {
            return effect;
        }

        public String getEffectLocalised() {
            return effectLocalised;
        }

        public String getTrend() {
            return trend;
        }
    }

    public static class Influence {
        @SerializedName("SystemAddress")
        private long systemAddress;

        @SerializedName("Trend")
        private String trend;

        @SerializedName("Influence")
        private String influence;

        public long getSystemAddress() {
            return systemAddress;
        }

        public String getTrend() {
            return trend;
        }

        public String getInfluence() {
            return influence;
        }
    }

    @Override
    public String toString() {
        return new StringJoiner("Mission completed: ")
                .add("faction='" + faction + "'")
                .add("name='" + name + "'")
                .add("localisedName='" + localisedName + "'")
                .add("missionID=" + missionID)
                .add("targetType='" + targetType + "'")
                .add("targetTypeLocalised='" + targetTypeLocalised + "'")
                .add("targetFaction='" + targetFaction + "'")
                .add("killCount=" + killCount)
                .add("destinationSystem='" + destinationSystem + "'")
                .add("destinationStation='" + destinationStation + "'")
                .add("reward=" + reward)
                .add("factionEffects=" + factionEffects)
                .toString();
    }
}