package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.TimestampFormatter;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;
import java.util.StringJoiner;

public class NpcCrewPaidWageEvent extends BaseEvent {
    @SerializedName("NpcCrewName")
    private String npcCrewName;

    @SerializedName("NpcCrewId")
    private long npcCrewId;

    @SerializedName("Amount")
    private long amount;

    public NpcCrewPaidWageEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "NpcCrewPaidWage");
        NpcCrewPaidWageEvent event = GsonFactory.getGson().fromJson(json, NpcCrewPaidWageEvent.class);
        this.npcCrewName = event.npcCrewName;
        this.npcCrewId = event.npcCrewId;
        this.amount = event.amount;
    }

    @Override
    public String getEventType() {
        return "NpcCrewPaidWage";
    }

    /** Routine automatic wage; noise. Ignore. */
    @Override
    public Importance importance() {
        return Importance.LOW;
    }

    @Override
    public String llmDescription() {
        return "Paid wages to an NPC crew member; carries the crew name and the amount.";
    }

    @Override
    public String toJson() {
        return GsonFactory.getGson().toJson(this);
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getNpcCrewName() {
        return npcCrewName;
    }

    public long getNpcCrewId() {
        return npcCrewId;
    }

    public long getAmount() {
        return amount;
    }

    public String getFormattedTimestamp(boolean useLocalTime) {
        return TimestampFormatter.formatTimestamp(getTimestamp().toString(), useLocalTime);
    }

    @Override
    public String toString() {
        return new StringJoiner("NPC crew wage paid: ")
                .add("npcCrewName='" + npcCrewName + "'")
                .add("npcCrewId=" + npcCrewId)
                .add("amount=" + amount)
                .toString();
    }
}