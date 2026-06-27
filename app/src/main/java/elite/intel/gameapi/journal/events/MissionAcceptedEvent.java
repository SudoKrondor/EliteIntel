package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class MissionAcceptedEvent extends BaseEvent {
    @SerializedName("Faction")
    private String faction;

    @SerializedName("Name")
    private String name;

    @SerializedName("LocalisedName")
    private String localisedName;

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

    @SerializedName("Expiry")
    private String expiry;

    @SerializedName("Wing")
    private boolean wing;

    @SerializedName("Influence")
    private String influence;

    @SerializedName("Reputation")
    private String reputation;

    @SerializedName("Reward")
    private long reward;

    @SerializedName("MissionID")
    private long missionID;

    @SerializedName("Commodity")
    private String commodity;

    @SerializedName("Target")
    private String target;

    @SerializedName("Count")
    private int count;

    @SerializedName("Commodity_Localised")
    private String commodityLocalised;

    @SerializedName("DestinationSettlement")
    private String destinationSettlement;

    public MissionAcceptedEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(60), "MissionAccepted");
        MissionAcceptedEvent event = GsonFactory.getGson().fromJson(json, MissionAcceptedEvent.class);
        this.faction = event.faction;
        this.name = event.name;
        this.localisedName = event.localisedName;
        this.targetType = event.targetType;
        this.targetTypeLocalised = event.targetTypeLocalised;
        this.targetFaction = event.targetFaction;
        this.killCount = event.killCount;
        this.destinationSystem = event.destinationSystem;
        this.destinationStation = event.destinationStation;
        this.expiry = event.expiry;
        this.wing = event.wing;
        this.influence = event.influence;
        this.reputation = event.reputation;
        this.reward = event.reward;
        this.missionID = event.missionID;
        this.target = event.target;
        this.count = event.count;
        this.commodityLocalised = event.commodityLocalised;
    }

    @Override
    public String getEventType() {
        return "MissionAccepted";
    }

    /** New objective worth acknowledging. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Accepted a mission; carries the mission name, the giving faction, destination, and reward.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getTarget() {
        return target;
    }

    public int getCount() {
        return count;
    }

    public String getCommodityLocalised() {
        return commodityLocalised;
    }

    public String getCommodity() {
        return commodity;
    }

    public String getDestinationSettlement() {
        return destinationSettlement;
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

    public String getExpiry() {
        return expiry;
    }

    public boolean isWing() {
        return wing;
    }

    public String getInfluence() {
        return influence;
    }

    public String getReputation() {
        return reputation;
    }

    public long getReward() {
        return reward;
    }

    public long getMissionID() {
        return missionID;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("Mission accepted: ")
                .add("faction='" + faction + "'")
                .add("name='" + name + "'")
                .add("localisedName='" + localisedName + "'")
                .add("targetType='" + targetType + "'")
                .add("targetTypeLocalised='" + targetTypeLocalised + "'")
                .add("targetFaction='" + targetFaction + "'")
                .add("killCount=" + killCount)
                .add("destinationSystem='" + destinationSystem + "'")
                .add("destinationStation='" + destinationStation + "'")
                .add("destinationSettlement='" + destinationSettlement + "'")
                .add("expiry='" + expiry + "'")
                .add("wing=" + wing)
                .add("influence='" + influence + "'")
                .add("reputation='" + reputation + "'")
                .add("reward=" + reward)
                .add("missionID=" + missionID)
                .add("target='" + target + "'")
                .add("count=" + count)
                .add("commodityName='" + commodityLocalised + "'")
                .toString();
    }
}