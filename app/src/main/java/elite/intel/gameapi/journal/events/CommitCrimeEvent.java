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

    /**
     * The journal reports a penalty as EITHER a bounty or a fine, never both: a murder carries
     * {@code Bounty}, while a lesser offence such as failing to submit to police carries {@code Fine}
     * and no bounty at all. Reading only the bounty announced every fine as "a bounty of zero credits".
     */
    @SerializedName("Fine")
    private long fine;

    public CommitCrimeEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "CommitCrime");
        CommitCrimeEvent event = GsonFactory.getGson().fromJson(json, CommitCrimeEvent.class);
        this.crimeType = event.crimeType;
        this.faction = event.faction;
        this.victim = event.victim;
        this.victimLocalised = event.victimLocalised;
        this.bounty = event.bounty;
        this.fine = event.fine;
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

    public long getFine() {
        return fine;
    }

    /**
     * The victim as a person is named, or {@code null} when the crime has no victim (a data transfer, an
     * illegal cargo scan). The localised name is the one to speak; the raw field is a
     * {@code $npc_name_decorate:#name=...;} token whenever the game supplied a localised one beside it,
     * and a plain name (an on-foot settlement worker) whenever it did not.
     */
    public String getSpokenVictim() {
        if (victimLocalised != null && !victimLocalised.isBlank()) return victimLocalised;
        if (victim == null || victim.isBlank()) return null;
        if (!victim.startsWith("$")) return victim;
        // A token reached us with no localised sibling. The readable name is the last "=name;" segment.
        int nameStart = victim.lastIndexOf('=');
        if (nameStart < 0) return null;
        String name = victim.substring(nameStart + 1).replace(";", "").trim();
        return name.isEmpty() ? null : name;
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
                .add("fine=" + fine)
                .toString();
    }
}