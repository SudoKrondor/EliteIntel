package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Paying off accumulated fines. {@code Amount} is an outflow.
 */
public class PayFinesEvent extends BaseEvent {

    @SerializedName("Amount")
    private long amount;
    @SerializedName("AllFines")
    private boolean allFines;
    @SerializedName("Faction")
    private String faction;

    public PayFinesEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "PayFines");
        PayFinesEvent e = GsonFactory.getGson().fromJson(json, PayFinesEvent.class);
        this.amount = e.amount;
        this.allFines = e.allFines;
        this.faction = e.faction;
    }

    @Override
    public String getEventType() {
        return "PayFines";
    }

    /** Routine fine payment; memory only. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Paid off outstanding fines; carries the amount.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getAmount() {
        return amount;
    }

    public boolean isAllFines() {
        return allFines;
    }

    public String getFaction() {
        return faction;
    }
}
