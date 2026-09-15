package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * A combat bond: the game paying the commander for a kill in a conflict zone, on behalf of the side
 * they fought for. The conflict-zone counterpart of {@link BountyEvent}.
 */
public class FactionKillBondEvent extends BaseEvent {

    @SerializedName("Reward")
    private long reward;

    @SerializedName("AwardingFaction")
    private String awardingFaction;

    @SerializedName("AwardingFaction_Localised")
    private String awardingFactionLocalised;

    @SerializedName("VictimFaction")
    private String victimFaction;

    @SerializedName("VictimFaction_Localised")
    private String victimFactionLocalised;

    public FactionKillBondEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "FactionKillBond");
        FactionKillBondEvent event = GsonFactory.getGson().fromJson(json, FactionKillBondEvent.class);
        this.reward = event.reward;
        this.awardingFaction = event.awardingFaction;
        this.awardingFactionLocalised = event.awardingFactionLocalised;
        this.victimFaction = event.victimFaction;
        this.victimFactionLocalised = event.victimFactionLocalised;
    }

    @Override
    public String getEventType() {
        return "FactionKillBond";
    }

    /**
     * NORMAL: kills in a conflict zone come every few seconds, and the HUD card carries the tally.
     * Kept in memory so VEGA can answer how the fight is going, but not spoken.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "You were awarded a combat bond for a kill in a conflict zone; carries the reward, the faction paying it and the faction of the ship destroyed.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getReward() {
        return reward;
    }

    /**
     * The side the commander fought for. Some factions (a power's forces, Thargoids) come as a
     * symbol with a localised twin; the plain name is preferred and the localised one fills in.
     */
    public String getAwardingFaction() {
        return awardingFaction;
    }

    public String getAwardingFactionLocalised() {
        return awardingFactionLocalised;
    }

    public String getVictimFaction() {
        return victimFaction;
    }

    public String getVictimFactionLocalised() {
        return victimFactionLocalised;
    }
}
