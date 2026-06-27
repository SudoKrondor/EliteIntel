package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.List;

public class BountyEvent extends BaseEvent {
    @SerializedName("Rewards")
    private List<Reward> rewards;

    @SerializedName("PilotName")
    private String pilotName;

    @SerializedName("PilotName_Localised")
    private String pilotNameLocalised;

    @SerializedName("Target")
    private String target;

    @SerializedName("TotalReward")
    private long totalReward;

    @SerializedName("VictimFaction")
    private String victimFaction;

    public BountyEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Bounty");
        BountyEvent event = GsonFactory.getGson().fromJson(json, BountyEvent.class);
        this.rewards = event.rewards;
        this.pilotName = event.pilotName;
        this.pilotNameLocalised = event.pilotNameLocalised;
        this.target = event.target;
        this.totalReward = event.totalReward;
        this.victimFaction = event.victimFaction;
    }

    @Override
    public String getEventType() {
        return "Bounty";
    }

    /**
     * NORMAL: BountyEventSubscriber already owns the spoken "kill confirmed / mission kill" callout
     * via EventNarrator, which now narrates in every mode. Kept in memory but off the consciousness's
     * spoken channel so the kill is not announced twice.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "You were awarded a bounty for destroying a wanted ship; carries the target ship, the awarding faction(s), and the total reward.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public String getPilotName() {
        return pilotName;
    }

    public String getPilotNameLocalised() {
        return pilotNameLocalised;
    }

    public String getTarget() {
        return target;
    }

    public long getTotalReward() {
        return totalReward;
    }

    public String getVictimFaction() {
        return victimFaction;
    }

    public static class Reward {
        @SerializedName("Faction")
        private String faction;

        @SerializedName("Reward")
        private long reward;

        public String getFaction() {
            return faction;
        }

        public long getReward() {
            return reward;
        }
    }
}