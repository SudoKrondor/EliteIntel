package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class CommitCrimeEvent extends BaseEvent {

    @SerializedName("CrimeType")
    private String crimeType;

    @SerializedName("Faction")
    private String faction;

    @SerializedName("Victim")
    private String victim;

    @SerializedName("Victim_Localised")
    private String victimLocalised;

    @SerializedName("Bounty")
    private long bounty;

    public CommitCrimeEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CommitCrime");
        CommitCrimeEvent event = GsonFactory.getGson().fromJson(json, CommitCrimeEvent.class);
        this.crimeType = event.crimeType;
        this.faction = event.faction;
        this.victim = event.victim;
        this.victimLocalised = event.victimLocalised;
        this.bounty = event.bounty;
    }

    @Override
    public String getEventType() {
        return "CommitCrime";
    }

    /**
     * NORMAL: CommitCrimeEventSubscriber already owns the spoken crime/bounty alert via EventNarrator,
     * which now narrates in every mode. Kept in memory but off the consciousness's spoken channel so
     * the crime is not announced twice.
     */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "You committed a crime (assault, murder, fine, and so on); carries the crime type, the victim faction, and any bounty or fine incurred.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getCrimeType() {
        return crimeType;
    }

    public String getFaction() {
        return faction;
    }

    public String getVictim() {
        return victim;
    }

    public String getVictimLocalised() {
        return victimLocalised;
    }

    public long getBounty() {
        return bounty;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "CommitCrime: ", "")
                .add("crimeType='" + crimeType + "'")
                .add("faction='" + faction + "'")
                .add("victim='" + (victimLocalised != null ? victimLocalised : victim) + "'")
                .add("bounty=" + bounty)
                .toString();
    }
}