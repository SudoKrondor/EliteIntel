package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Paying off accumulated bounties. {@code Amount} is an outflow.
 */
public class PayBountiesEvent extends BaseEvent {

    @SerializedName("Amount")
    private long amount;
    // FDev reuses the "AllFines" key for PayBounties (documented journal quirk), not "AllBounties".
    @SerializedName("AllFines")
    private boolean allBounties;
    @SerializedName("Faction")
    private String faction;

    public PayBountiesEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "PayBounties");
        PayBountiesEvent e = GsonFactory.getGson().fromJson(json, PayBountiesEvent.class);
        this.amount = e.amount;
        this.allBounties = e.allBounties;
        this.faction = e.faction;
    }

    @Override
    public String getEventType() {
        return "PayBounties";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public long getAmount() {
        return amount;
    }

    public boolean isAllBounties() {
        return allBounties;
    }

    public String getFaction() {
        return faction;
    }
}
