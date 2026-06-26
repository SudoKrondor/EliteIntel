package elite.intel.gameapi.journal.events;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import elite.intel.util.json.GsonFactory;

import java.time.Duration;

/**
 * Fired when the commander pays a rebuy after destruction. {@code Cost} is the
 * rebuy charge (an outflow). When {@code Bankrupt} is true the rebuy could not be
 * paid, so no credits are deducted.
 */
public class ResurrectEvent extends BaseEvent {

    @SerializedName("Option")
    private String option;
    @SerializedName("Cost")
    private long cost;
    @SerializedName("Bankrupt")
    private boolean bankrupt;

    public ResurrectEvent(JsonObject json) {
        super(json.get("timestamp").getAsString(), Duration.ofSeconds(30), "Resurrect");
        ResurrectEvent e = GsonFactory.getGson().fromJson(json, ResurrectEvent.class);
        this.option = e.option;
        this.cost = e.cost;
        this.bankrupt = e.bankrupt;
    }

    @Override
    public String getEventType() {
        return "Resurrect";
    }

    /** Death and respawn; a major moment. */
    @Override
    public Importance importance() {
        return Importance.NORMAL;
    }

    @Override
    public String llmDescription() {
        return "Respawned after death (rebuy, recover, or hand in); carries the option chosen and the cost.";
    }

    @Override
    public JsonObject toJsonObject() {
        return GsonFactory.toJsonObject(this);
    }

    public String getOption() {
        return option;
    }

    public long getCost() {
        return cost;
    }

    public boolean isBankrupt() {
        return bankrupt;
    }
}
